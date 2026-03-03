package com.redcheck.backend.repository;

import com.redcheck.backend.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findAllByUserId(Long userId);
}
