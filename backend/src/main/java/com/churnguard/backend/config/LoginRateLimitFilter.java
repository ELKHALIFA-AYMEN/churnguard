package com.churnguard.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limite le nombre de requêtes POST sur /api/auth/ par adresse IP (fenêtre glissante).
 * Protège la connexion contre la force brute et l'inscription contre la création en masse.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_ADDRESSES = 10_000;

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> attemptsByAddress = new ConcurrentHashMap<>();

    @Autowired
    public LoginRateLimitFilter(
            @Value("${app.security.login-rate-limit.max-attempts:10}") int maxAttempts,
            @Value("${app.security.login-rate-limit.window-seconds:60}") long windowSeconds) {
        this(maxAttempts, Duration.ofSeconds(windowSeconds), Clock.systemUTC());
    }

    LoginRateLimitFilter(int maxAttempts, Duration window, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean concerned = "POST".equals(request.getMethod())
                && request.getRequestURI().startsWith("/api/auth/");
        return !concerned;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long secondsToWait = tryAcquire(request.getRemoteAddr());
        if (secondsToWait > 0) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(secondsToWait));
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"message\":\"Trop de tentatives. Réessayez plus tard.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    /** Renvoie 0 si la requête est autorisée, sinon le nombre de secondes à attendre. */
    private long tryAcquire(String address) {
        Instant now = clock.instant();
        Instant threshold = now.minus(window);

        if (attemptsByAddress.size() > MAX_TRACKED_ADDRESSES) {
            evictExpired(threshold);
        }

        Deque<Instant> attempts = attemptsByAddress.computeIfAbsent(address, key -> new ArrayDeque<>());
        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(threshold)) {
                attempts.pollFirst();
            }
            if (attempts.size() >= maxAttempts) {
                long wait = Duration.between(now, attempts.peekFirst().plus(window)).toSeconds() + 1;
                return Math.max(wait, 1);
            }
            attempts.addLast(now);
            return 0;
        }
    }

    /** Évite que la mémoire grossisse indéfiniment si beaucoup d'adresses différentes passent. */
    private void evictExpired(Instant threshold) {
        attemptsByAddress.entrySet().removeIf(entry -> {
            Deque<Instant> attempts = entry.getValue();
            synchronized (attempts) {
                return attempts.isEmpty() || attempts.peekLast().isBefore(threshold);
            }
        });
    }
}