package com.churnguard.backend.service;

import com.churnguard.backend.domain.Organization;
import com.churnguard.backend.domain.Role;
import com.churnguard.backend.domain.User;
import com.churnguard.backend.dto.RegisterRequest;
import com.churnguard.backend.exception.EmailAlreadyUsedException;
import com.churnguard.backend.repository.OrganizationRepository;
import com.churnguard.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        Organization organization =
                organizationRepository.save(new Organization(request.organizationName().trim()));

        String passwordHash = passwordEncoder.encode(request.password());

        return userRepository.save(new User(organization, email, passwordHash, Role.ADMIN));
    }
}