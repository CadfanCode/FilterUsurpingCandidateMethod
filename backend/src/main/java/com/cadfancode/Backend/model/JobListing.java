package com.cadfancode.Backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class JobListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId;
    private String title;
    private String company;
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String url;
    private String salary;
    private String source;

    private LocalDateTime discoveredAt;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus applicationStatus = ApplicationStatus.FOUND;

    public enum ApplicationStatus {
        FOUND, APPLIED, REJECTED, SAVED
    }

    @PrePersist
    public void prePersist() {
        discoveredAt = LocalDateTime.now();
    }
}
