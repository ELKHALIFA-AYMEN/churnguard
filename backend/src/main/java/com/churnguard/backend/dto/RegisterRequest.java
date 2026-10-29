package com.churnguard.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Size(max = 150)
        String organizationName,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 12, max = 72)
        String password
) {

    @Override
    public String toString() {
        return "RegisterRequest[organizationName=" + organizationName + ", email=" + email + "]";
    }
}