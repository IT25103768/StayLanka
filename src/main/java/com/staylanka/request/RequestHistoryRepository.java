package com.staylanka.request;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {
    @EntityGraph(attributePaths = "changedBy")
    List<RequestHistory> findByRequestIdOrderByCreatedAtAsc(Long requestId);
}

