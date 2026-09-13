package com.redcheck.backend.repository;

import com.redcheck.backend.entity.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventCategoryRepository extends JpaRepository<EventCategory, Long> {

    List<EventCategory> findAllByUser_Id(Long userId);
}
