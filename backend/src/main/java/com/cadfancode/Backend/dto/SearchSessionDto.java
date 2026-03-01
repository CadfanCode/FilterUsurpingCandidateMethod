package com.cadfancode.Backend.dto;

import com.cadfancode.Backend.model.SearchSession;

public record SearchSessionDto(
        String id,
        SearchSession.Status status,
        int jobsFound,
        String errorMessage
) {
    public static SearchSessionDto from(SearchSession session) {
        return new SearchSessionDto(
                session.getId(),
                session.getStatus(),
                session.getJobsFound(),
                session.getErrorMessage()
        );
    }
}
