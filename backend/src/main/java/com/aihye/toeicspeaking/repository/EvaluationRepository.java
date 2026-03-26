package com.aihye.toeicspeaking.repository;

import com.aihye.toeicspeaking.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Integer> {

    List<Evaluation> findByResponseIdOrderByEvaluatedAtDesc(Integer responseId);

    Optional<Evaluation> findTopByResponseIdOrderByEvaluatedAtDesc(Integer responseId);
}
