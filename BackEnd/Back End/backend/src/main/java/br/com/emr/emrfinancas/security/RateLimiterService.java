package br.com.emr.emrfinancas.security;

import br.com.emr.emrfinancas.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiterService.class);

    private final Clock clock;
    private final boolean exemptLocal;
    private final Map<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();

    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_LOGIN_IP_EMAIL = 5;
    private static final int MAX_LOGIN_IP = 30;
    private static final int MAX_FORGOT_PASSWORD_IP = 5;
    private static final int MAX_REGISTER_IP = 5;
    private static final int MAX_RESET_PASSWORD_IP = 10;

    public RateLimiterService(Clock clock,
                              @org.springframework.beans.factory.annotation.Value("${app.security.rate-limit.exempt-local:false}") boolean exemptLocal) {
        this.clock = clock;
        this.exemptLocal = exemptLocal;
    }

    private boolean isLocalExempt(String ip) {
        if (!exemptLocal) return false;
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "localhost".equalsIgnoreCase(ip);
    }

    public void checkLoginRateLimit(String ip, String normalizedEmail) {
        if (isLocalExempt(ip)) return;
        checkLimit("login:ip:" + ip, MAX_LOGIN_IP, DEFAULT_WINDOW);
        if (normalizedEmail != null && !normalizedEmail.isBlank()) {
            checkLimit("login:ip-email:" + ip + ":" + normalizedEmail, MAX_LOGIN_IP_EMAIL, DEFAULT_WINDOW);
        }
    }

    public void checkForgotPasswordRateLimit(String ip) {
        if (isLocalExempt(ip)) return;
        checkLimit("forgot-password:ip:" + ip, MAX_FORGOT_PASSWORD_IP, DEFAULT_WINDOW);
    }

    public void checkRegisterRateLimit(String ip) {
        if (isLocalExempt(ip)) return;
        checkLimit("register:ip:" + ip, MAX_REGISTER_IP, DEFAULT_WINDOW);
    }

    public void checkResetPasswordRateLimit(String ip) {
        if (isLocalExempt(ip)) return;
        checkLimit("reset-password:ip:" + ip, MAX_RESET_PASSWORD_IP, DEFAULT_WINDOW);
    }

    public synchronized void checkLimit(String key, int maxRequests, Duration window) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(window);

        Deque<Instant> timestamps = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequests) {
            LOGGER.warn("Rate limit excedido para a chave {}", sanitizeKeyForLog(key));
            throw new RateLimitExceededException("Muitas tentativas. Tente novamente mais tarde.");
        }

        timestamps.addLast(now);

        // Limpeza oportunista de chaves vazias se o mapa crescer
        if (buckets.size() > 500) {
            cleanupExpired(cutoff);
        }
    }

    public synchronized void clear() {
        buckets.clear();
    }

    private synchronized void cleanupExpired(Instant cutoff) {
        buckets.entrySet().removeIf(entry -> {
            Deque<Instant> deque = entry.getValue();
            while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.pollFirst();
            }
            return deque.isEmpty();
        });
    }

    private String sanitizeKeyForLog(String key) {
        int colonIdx = key.indexOf(':');
        if (colonIdx > 0) {
            return key.substring(0, colonIdx);
        }
        return "secure-key";
    }
}
