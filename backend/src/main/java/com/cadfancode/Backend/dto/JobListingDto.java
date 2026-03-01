package com.cadfancode.Backend.dto;

public record JobListingDto(
        String title,
        String company,
        String location,
        String description,
        String url,
        String salary,
        String source
) {}
