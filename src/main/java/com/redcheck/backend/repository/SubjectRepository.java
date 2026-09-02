package com.redcheck.backend.repository;

import com.redcheck.backend.entity.Subject;
import com.redcheck.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findAllByUser(User user);

    boolean existsByNameAndUser(String name, User user);

    boolean existsByNameAndUserAndIdNot(String name, User user, Long id);

    @Modifying
    @Query("DELETE FROM Subject s WHERE s.deleted = true AND s.deletedAt < :cutoffDate")
    void deleteTrashOlderThan(LocalDateTime cutoffDate);

    List<Subject> findAllByUserAndDeletedTrue(User user);

    List<Subject> findAllByUserAndDeletedFalse(User user);

    List<Subject> findAllByUserAndArchivedAndDeletedFalse(User user, boolean archived);
}