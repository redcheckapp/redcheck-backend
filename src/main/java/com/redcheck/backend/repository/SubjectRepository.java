package com.redcheck.backend.repository;

import com.redcheck.backend.entity.Subject;
import com.redcheck.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findAllByUser(User user);

    List<Subject> findAllByUserAndArchived(User use, boolean archived);

    boolean existsByNameAndUser(String name, User user);

    boolean existsByNameAndUserAndIdNot(String name, User user, Long id);
}
