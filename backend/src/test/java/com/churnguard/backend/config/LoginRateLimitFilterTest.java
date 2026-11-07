package com.churnguard.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoginRateLimitFilterTest {

    private static final String LOGIN = "/api/auth/login";

    private final MutableClock clock = new MutableClock();
    private final LoginRateLimitFilter filter =
            new LoginRateLimitFilter(3, Duration.ofSeconds(60), clock);

    private MockHttpServletResponse send(String method, String path, String address) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(address);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void blocksAfterMaxAttemptsAndTellsHowLongToWait() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(send("POST", LOGIN, "10.0.0.1").getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blocked = send("POST", LOGIN, "10.0.0.1");

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotNull();
        assertThat(Long.parseLong(blocked.getHeader("Retry-After"))).isBetween(1L, 61L);
    }

    @Test
    void allowsAgainOnceTheWindowHasPassed() throws Exception {
        for (int i = 0; i < 3; i++) {
            send("POST", LOGIN, "10.0.0.1");
        }
        assertThat(send("POST", LOGIN, "10.0.0.1").getStatus()).isEqualTo(429);

        clock.advance(Duration.ofSeconds(61));

        assertThat(send("POST", LOGIN, "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void limitsEachAddressSeparately() throws Exception {
        for (int i = 0; i < 4; i++) {
            send("POST", LOGIN, "10.0.0.1");
        }

        assertThat(send("POST", LOGIN, "10.0.0.1").getStatus()).isEqualTo(429);
        assertThat(send("POST", LOGIN, "10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void ignoresOtherRoutesAndMethods() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(send("GET", LOGIN, "10.0.0.1").getStatus()).isEqualTo(200);
            assertThat(send("POST", "/api/customers/import", "10.0.0.1").getStatus()).isEqualTo(200);
        }
    }

    /** Horloge dont on contrôle l'heure, pour tester sans attendre. */
    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}