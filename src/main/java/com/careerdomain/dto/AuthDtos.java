package com.careerdomain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() { }
    public record RegisterRequest(@NotBlank @Size(max = 100) String name,
                                  @NotBlank @Email @Size(max = 254) String email,
                                  @NotBlank @Size(min = 8, max = 100) String password) { }
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }
    public record UserResponse(Long id, String name, String email) { }
    public record LoginResponse(String token, UserResponse user) { }
}
