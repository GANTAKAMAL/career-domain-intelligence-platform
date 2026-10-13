package com.careerdomain.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import com.careerdomain.service.UserService;
import com.careerdomain.security.TokenService;
import com.careerdomain.dto.AuthDtos.*;

@RestController @RequestMapping("/auth")
public class AuthController {
    private final UserService userService; private final TokenService tokens;
    public AuthController(UserService userService, TokenService tokens) { this.userService = userService; this.tokens = tokens; }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        var user = userService.register(request.name(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(response(user));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) { return response(userService.login(request.email(), request.password())); }
    private LoginResponse response(com.careerdomain.entity.User user) { return new LoginResponse(tokens.create(user.getEmail()), new UserResponse(user.getId(), user.getName(), user.getEmail())); }
}
