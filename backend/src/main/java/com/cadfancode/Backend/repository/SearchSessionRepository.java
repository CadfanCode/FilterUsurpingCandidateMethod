package com.cadfancode.Backend.repository;

import com.cadfancode.Backend.model.SearchSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchSessionRepository extends JpaRepository<SearchSession, String> {
}
