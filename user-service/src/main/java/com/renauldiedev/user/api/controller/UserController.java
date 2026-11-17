package com.renauldiedev.user.api.controller;

import com.renauldiedev.user.api.dto.ApiResponse;
import com.renauldiedev.user.api.dto.UserProfileResponse;
import com.renauldiedev.user.domain.user.AuthService;
import com.renauldiedev.user.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "Endpoints for user profile and information")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @RequestHeader(value = "X-User-Id") Long userId) {
        User user = authService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(toProfileResponse(user)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user profile by ID")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(
            @PathVariable Long id) {
        User user = authService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(toProfileResponse(user)));
    }

    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
