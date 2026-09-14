package com.staylanka.request;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestResponseRepository extends JpaRepository<RequestResponse, Long> {
    @EntityGraph(attributePaths = "author")
    List<RequestResponse> findByRequestIdOrderByCreatedAtAsc(Long requestId);

    void deleteByRequestId(Long requestId);
}

