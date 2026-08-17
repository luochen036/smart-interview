package com.luochen.userservice.repository;

import com.luochen.userservice.entity.InterviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InterviewRepository extends JpaRepository<InterviewRecord, Long> {
    List<InterviewRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}