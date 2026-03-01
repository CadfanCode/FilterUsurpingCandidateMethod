package com.cadfancode.Backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class TavilyService {

    private static final Logger log = LoggerFactory.getLogger(TavilyService.class);
    private static final String TAVILY_URL = "https://api.tavily.com/search";

    @Value("${tavily.api-key:}")
    private String apiKey;

    private final RestClient restClient;

    public TavilyService() {
        this.restClient = RestClient.builder()
                .baseUrl(TAVILY_URL)
                .build();
    }

    public String search(String query) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "api_key", apiKey,
                    "query", query,
                    "search_depth", "basic",
                    "max_results", 8,
                    "include_answer", false,
                    "include_raw_content", false,
                    "include_images", false
            );

            String response = restClient.post()
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return response != null ? response : "{}";
        } catch (Exception e) {
            log.warn("Tavily search failed for query '{}': {}", query, e.getMessage());
            return "{}";
        }
    }
}
