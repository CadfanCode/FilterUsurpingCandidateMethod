package com.cadfancode.Backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchRequestDto {

    @NotBlank
    private String jobTitle;

    @NotBlank
    private String location;

    @NotBlank
    private String resumeText;
}
