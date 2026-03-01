package com.cadfancode.Backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class SearchSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String jobTitle;
    private String location;

    @Column(columnDefinition = "TEXT")
    private String resumeText;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private String errorMessage;
    private int jobsFound;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public enum Status {
        PENDING, RUNNING, COMPLETED, FAILED
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
