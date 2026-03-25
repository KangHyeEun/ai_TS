package com.aihye.toeicspeaking.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${upload.path}")
    private String uploadPath;

    @PostMapping("/audio")
    public ResponseEntity<?> uploadAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Integer userId,
            @RequestParam("partNumber") Integer partNumber,
            @RequestParam("questionIdx") Integer questionIdx) {

        try {
            Path audioDir = Paths.get(uploadPath, "audio").toAbsolutePath();
            Files.createDirectories(audioDir);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("user%d_part%d_q%d_%s.webm",
                    userId, partNumber, questionIdx, timestamp);

            Path filePath = audioDir.resolve(filename);
            file.transferTo(filePath.toFile());

            String fileUrl = "/uploads/audio/" + filename;

            return ResponseEntity.ok(Map.of(
                "filename", filename,
                "url", fileUrl,
                "message", "오디오 저장 완료"
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "오디오 저장 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "questionId", required = false) Integer questionId) {

        try {
            Path imageDir = Paths.get(uploadPath, "images").toAbsolutePath();
            Files.createDirectories(imageDir);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : ".png";
            String filename = String.format("question_%s%s", timestamp, ext);

            Path filePath = imageDir.resolve(filename);
            file.transferTo(filePath.toFile());

            String fileUrl = "/uploads/images/" + filename;

            return ResponseEntity.ok(Map.of(
                "filename", filename,
                "url", fileUrl,
                "message", "이미지 저장 완료"
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "이미지 저장 실패: " + e.getMessage()));
        }
    }
}
