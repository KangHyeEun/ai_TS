package com.aihye.toeicspeaking.controller;

import com.aihye.toeicspeaking.entity.User;
import com.aihye.toeicspeaking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/current")
    public Map<String, Object> getCurrentUser() {
        User user = userService.getCurrentUser();
        return Map.of(
            "userId", user.getUserId(),
            "email", user.getEmail(),
            "nickname", user.getNickname(),
            "targetScore", user.getTargetScore()
        );
    }
}
