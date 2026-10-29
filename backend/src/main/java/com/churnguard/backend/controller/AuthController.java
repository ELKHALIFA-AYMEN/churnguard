package com.churnguard.backend.controller;

import com.churnguard.backend.domain.User;
import com.churnguard.backend.dto.LoginRequest;
import com.churnguard.backend.dto.LoginResponse;
import com.churnguard.backend.dto.RegisterRequest;
import com.churnguard.backend.dto.UserResponse;
import com.churnguard.backend.service.AuthService;
import com.churnguard.backend.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(authService.register(request));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request);
        return new LoginResponse(jwtService.generateToken(user), "Bearer");
    }
}