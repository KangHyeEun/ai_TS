package com.aihye.toeicspeaking.service;

import com.aihye.toeicspeaking.dto.RecordRequest;
import com.aihye.toeicspeaking.entity.PracticeRecord;
import com.aihye.toeicspeaking.repository.RecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final RecordRepository recordRepository;

    public List<PracticeRecord> getAllRecords() {
        return recordRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public PracticeRecord createRecord(RecordRequest request) {
        PracticeRecord record = new PracticeRecord();
        record.setPartId(request.getPartId());
        record.setPartTitle(request.getPartTitle());
        record.setQuestionIdx(request.getQuestionIdx());
        return recordRepository.save(record);
    }

    @Transactional
    public void deleteAllRecords() {
        recordRepository.deleteAll();
    }
}
