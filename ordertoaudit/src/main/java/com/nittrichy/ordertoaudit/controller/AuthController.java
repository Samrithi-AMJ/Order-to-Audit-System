package com.nittrichy.ordertoaudit.controller;

import com.nittrichy.ordertoaudit.dto.AuthResponse;
import com.nittrichy.ordertoaudit.dto.LoginRequest;
import com.nittrichy.ordertoaudit.dto.RegisterRequest;
import com.nittrichy.ordertoaudit.dto.UserDto;
import com.nittrichy.ordertoaudit.entity.User;
import com.nittrichy.ordertoaudit.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(authService.mapToDto(user));
    }
}

