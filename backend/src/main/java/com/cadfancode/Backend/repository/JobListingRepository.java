package com.cadfancode.Backend.repository;

import com.cadfancode.Backend.model.JobListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobListingRepository extends JpaRepository<JobListing, Long> {

    List<JobListing> findBySessionId(String sessionId);

    Page<JobListing> findAll(Pageable pageable);
}
