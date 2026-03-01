package com.cadfancode.Backend.controller;

import com.cadfancode.Backend.dto.SearchRequestDto;
import com.cadfancode.Backend.dto.SearchSessionDto;
import com.cadfancode.Backend.model.JobListing;
import com.cadfancode.Backend.model.SearchSession;
import com.cadfancode.Backend.repository.JobListingRepository;
import com.cadfancode.Backend.repository.SearchSessionRepository;
import com.cadfancode.Backend.service.JobSearchService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class JobController {

    private final SearchSessionRepository sessionRepository;
    private final JobListingRepository jobListingRepository;
    private final JobSearchService jobSearchService;

    public JobController(
            SearchSessionRepository sessionRepository,
            JobListingRepository jobListingRepository,
            JobSearchService jobSearchService
    ) {
        this.sessionRepository = sessionRepository;
        this.jobListingRepository = jobListingRepository;
        this.jobSearchService = jobSearchService;
    }

    @PostMapping("/search/start")
    public ResponseEntity<?> startSearch(@Valid @RequestBody SearchRequestDto request) {
        SearchSession session = new SearchSession();
        session.setJobTitle(request.getJobTitle());
        session.setLocation(request.getLocation());
        session.setResumeText(request.getResumeText());
        session.setStatus(SearchSession.Status.PENDING);
        session = sessionRepository.save(session);

        jobSearchService.startSearch(session.getId());

        return ResponseEntity.ok(Map.of("sessionId", session.getId()));
    }

    @GetMapping("/search/{id}/status")
    public ResponseEntity<?> getStatus(@PathVariable String id) {
        return sessionRepository.findById(id)
                .map(s -> ResponseEntity.ok(SearchSessionDto.from(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/jobs")
    public ResponseEntity<?> getJobs(
            @RequestParam(required = false) String sessionId,
            Pageable pageable
    ) {
        if (sessionId != null && !sessionId.isBlank()) {
            List<JobListing> jobs = jobListingRepository.findBySessionId(sessionId);
            return ResponseEntity.ok(jobs);
        }
        Page<JobListing> page = jobListingRepository.findAll(pageable);
        return ResponseEntity.ok(page);
    }

    @PatchMapping("/jobs/{id}/status")
    public ResponseEntity<?> updateJobStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String statusStr = body.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "status field required"));
        }

        JobListing.ApplicationStatus newStatus;
        try {
            newStatus = JobListing.ApplicationStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + statusStr));
        }

        return jobListingRepository.findById(id)
                .map(job -> {
                    job.setApplicationStatus(newStatus);
                    return ResponseEntity.ok(jobListingRepository.save(job));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
