package com.aihye.toeicspeaking.controller;

import com.aihye.toeicspeaking.dto.RecordRequest;
import com.aihye.toeicspeaking.entity.PracticeRecord;
import com.aihye.toeicspeaking.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @GetMapping
    public List<PracticeRecord> getAll() {
        return recordService.getAllRecords();
    }

    @PostMapping
    public ResponseEntity<Map<String, Integer>> create(@RequestBody RecordRequest request) {
        PracticeRecord record = recordService.createRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", record.getId()));
    }

    @DeleteMapping
    public Map<String, String> deleteAll() {
        recordService.deleteAllRecords();
        return Map.of("message", "전체 기록 삭제 완료");
    }
}
