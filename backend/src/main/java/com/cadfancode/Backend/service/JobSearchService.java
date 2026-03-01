package com.cadfancode.Backend.service;

import com.cadfancode.Backend.dto.JobListingDto;
import com.cadfancode.Backend.model.JobListing;
import com.cadfancode.Backend.model.SearchSession;
import com.cadfancode.Backend.repository.JobListingRepository;
import com.cadfancode.Backend.repository.SearchSessionRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class JobSearchService {

    private static final Logger log = LoggerFactory.getLogger(JobSearchService.class);

    private final SearchSessionRepository sessionRepository;
    private final JobListingRepository jobListingRepository;
    private final TavilyService tavilyService;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public JobSearchService(
            SearchSessionRepository sessionRepository,
            JobListingRepository jobListingRepository,
            TavilyService tavilyService,
            ChatClient.Builder chatClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.jobListingRepository = jobListingRepository;
        this.tavilyService = tavilyService;
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Async
    public void startSearch(String sessionId) {
        SearchSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        try {
            session.setStatus(SearchSession.Status.RUNNING);
            sessionRepository.save(session);

            // Phase 1: Generate search queries via GPT-4o
            log.info("[{}] Generating search queries...", sessionId);
            List<String> queries = generateSearchQueries(
                    session.getJobTitle(), session.getLocation(), session.getResumeText());
            log.info("[{}] Generated {} queries", sessionId, queries.size());

            // Phase 2: Run all queries against Tavily in parallel
            log.info("[{}] Executing Tavily searches...", sessionId);
            List<CompletableFuture<String>> futures = queries.stream()
                    .map(query -> CompletableFuture.supplyAsync(() -> tavilyService.search(query)))
                    .collect(Collectors.toList());

            List<String> rawResults = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(r -> !r.equals("{}"))
                    .collect(Collectors.toList());
            log.info("[{}] Collected {} non-empty results", sessionId, rawResults.size());

            // Phase 3: Extract and deduplicate job listings via GPT-4o structured output
            log.info("[{}] Extracting job listings...", sessionId);
            List<JobListingDto> listings = extractJobListings(
                    rawResults, session.getJobTitle(), session.getLocation());
            log.info("[{}] Extracted {} listings", sessionId, listings.size());

            // Persist
            List<JobListing> entities = listings.stream()
                    .map(dto -> toEntity(dto, sessionId))
                    .collect(Collectors.toList());
            jobListingRepository.saveAll(entities);

            session.setJobsFound(entities.size());
            session.setStatus(SearchSession.Status.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);
            log.info("[{}] Search completed, {} jobs saved", sessionId, entities.size());

        } catch (Exception e) {
            log.error("[{}] Search failed: {}", sessionId, e.getMessage(), e);
            session.setStatus(SearchSession.Status.FAILED);
            session.setErrorMessage(e.getMessage());
            session.setCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
    }

    private List<String> generateSearchQueries(String jobTitle, String location, String resumeText) {
        String resumeSummary = resumeText.length() > 2000
                ? resumeText.substring(0, 2000) + "..."
                : resumeText;

        String prompt = """
                You are a job search expert. Given the following information:
                - Job title seeking: %s
                - Location: %s
                - Resume excerpt: %s

                Generate exactly 8 diverse search queries to find real job listings on LinkedIn, Indeed, \
                Glassdoor, and other job boards. Include variations of the job title, related roles, \
                and different search angles (e.g. remote, entry-level, senior, related tech stack).

                Return ONLY a valid JSON array of strings with no markdown fences or explanation.
                Example: ["query one", "query two", ...]
                """.formatted(jobTitle, location, resumeSummary);

        String responseContent = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        try {
            // Strip any markdown code fences if present
            String cleaned = responseContent.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```[a-z]*\\n?", "").replace("```", "").strip();
            }
            return objectMapper.readValue(cleaned, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse query generation response, using fallback queries. Response: {}", responseContent);
            return List.of(
                    jobTitle + " jobs " + location,
                    jobTitle + " job listing " + location,
                    "\"" + jobTitle + "\" site:linkedin.com " + location,
                    "\"" + jobTitle + "\" site:indeed.com " + location
            );
        }
    }

    private List<JobListingDto> extractJobListings(List<String> rawResults, String jobTitle, String location) {
        if (rawResults.isEmpty()) {
            return List.of();
        }

        String combinedResults = String.join("\n\n---\n\n", rawResults);
        if (combinedResults.length() > 15000) {
            combinedResults = combinedResults.substring(0, 15000) + "\n...[truncated]";
        }

        String prompt = """
                Extract all job listings from the following web search results.

                For each job posting found, extract these fields:
                - title: job title
                - company: company name
                - location: job location
                - description: brief description (max 300 chars)
                - url: direct link to the job posting
                - salary: salary/pay range if mentioned, otherwise null
                - source: name of the job site (e.g. LinkedIn, Indeed, Glassdoor)

                Rules:
                - Only include actual job postings, skip news articles or generic pages
                - Deduplicate: skip entries where company + title already appeared
                - If a field is unavailable use null
                - Target job: %s in %s

                Return ONLY a valid JSON array of job objects matching the schema above.
                If no jobs are found return an empty array [].

                Search results:
                %s
                """.formatted(jobTitle, location, combinedResults);

        try {
            List<JobListingDto> listings = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(new ParameterizedTypeReference<List<JobListingDto>>() {});
            return listings != null ? listings : List.of();
        } catch (Exception e) {
            log.warn("Structured output extraction failed, attempting manual parse: {}", e.getMessage());
            // Fallback: get raw content and parse manually
            try {
                String raw = chatClient.prompt().user(prompt).call().content();
                if (raw != null) {
                    String cleaned = raw.strip();
                    if (cleaned.startsWith("```")) {
                        cleaned = cleaned.replaceAll("```[a-z]*\\n?", "").replace("```", "").strip();
                    }
                    return objectMapper.readValue(cleaned, new TypeReference<List<JobListingDto>>() {});
                }
            } catch (Exception ex) {
                log.error("Manual parse also failed: {}", ex.getMessage());
            }
            return new ArrayList<>();
        }
    }

    private JobListing toEntity(JobListingDto dto, String sessionId) {
        JobListing entity = new JobListing();
        entity.setSessionId(sessionId);
        entity.setTitle(dto.title());
        entity.setCompany(dto.company());
        entity.setLocation(dto.location());
        entity.setDescription(dto.description());
        entity.setUrl(dto.url());
        entity.setSalary(dto.salary());
        entity.setSource(dto.source());
        return entity;
    }
}
