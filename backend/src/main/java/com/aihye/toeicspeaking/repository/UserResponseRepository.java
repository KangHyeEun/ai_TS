package com.aihye.toeicspeaking.repository;

import com.aihye.toeicspeaking.entity.UserResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserResponseRepository extends JpaRepository<UserResponse, Integer> {

    List<UserResponse> findByUserIdOrderBySubmittedAtDesc(Integer userId);

    long countByUserIdAndQuestionId(Integer userId, Integer questionId);
}
