package com.aihye.toeicspeaking.repository;

import com.aihye.toeicspeaking.entity.PracticeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<PracticeRecord, Integer> {

    List<PracticeRecord> findAllByOrderByCreatedAtDesc();
}
