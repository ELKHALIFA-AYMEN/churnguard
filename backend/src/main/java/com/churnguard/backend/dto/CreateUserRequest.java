package com.churnguard.backend.dto;

import com.churnguard.backend.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 12, max = 72)
        String password,

        @NotNull
        Role role
) {

    @Override
    public String toString() {
        return "CreateUserRequest[email=" + email + ", role=" + role + "]";
    }
}