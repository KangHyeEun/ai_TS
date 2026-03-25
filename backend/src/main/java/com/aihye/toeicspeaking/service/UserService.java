package com.aihye.toeicspeaking.service;

import com.aihye.toeicspeaking.entity.User;
import com.aihye.toeicspeaking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 기본 사용자 (김학생, user_id=1) 자동 로그인
    private static final Integer DEFAULT_USER_ID = 1;

    public User getCurrentUser() {
        return userRepository.findById(DEFAULT_USER_ID)
                .orElseThrow(() -> new RuntimeException("기본 사용자를 찾을 수 없습니다."));
    }
}
