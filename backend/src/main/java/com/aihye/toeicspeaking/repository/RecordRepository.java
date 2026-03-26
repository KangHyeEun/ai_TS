package com.aihye.toeicspeaking.repository;

import com.aihye.toeicspeaking.entity.PracticeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

@Repository
public interface RecordRepository extends JpaRepository<PracticeRecord, Integer> {

    List<PracticeRecord> findAllByOrderByCreatedAtDesc();

    List<PracticeRecord> findByUserIdOrderByCreatedAtDesc(Integer userId);

    @Query("SELECT DISTINCT r.questionId FROM PracticeRecord r WHERE r.userId = :userId AND r.questionId IS NOT NULL")
    Set<Integer> findSolvedQuestionIdsByUserId(Integer userId);
}
