package com.churnguard.backend.service;

import com.churnguard.backend.domain.Organization;
import com.churnguard.backend.domain.Role;
import com.churnguard.backend.domain.User;
import com.churnguard.backend.dto.LoginRequest;
import com.churnguard.backend.dto.RegisterRequest;
import com.churnguard.backend.exception.EmailAlreadyUsedException;
import com.churnguard.backend.exception.InvalidCredentialsException;
import com.churnguard.backend.repository.OrganizationRepository;
import com.churnguard.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
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

    @Transactional(readOnly = true)
    public User login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Optional<User> found = userRepository.findByEmail(email);

        String hashToCheck = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (found.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        return found.get();
    }
}