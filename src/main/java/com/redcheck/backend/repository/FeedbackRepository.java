package com.redcheck.backend.repository;

import com.redcheck.backend.entity.Feedback;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // Fetches the (lazy) user in the same query — the dashboard shows the
    // author's email on every row, so without this it'd be an N+1.
    @EntityGraph(attributePaths = "user")
    List<Feedback> findAllByOrderByCreatedDateDesc();
}
