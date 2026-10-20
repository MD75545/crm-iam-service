package com.crm.service;

import com.crm.entity.RefreshToken;
import com.crm.entity.User;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.RefreshTokenRepository;
import com.crm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private Long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RefreshToken createRefreshToken(String email) {
        log.debug("Creating refresh token for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Cannot create refresh token – user not found: {}", email);
                    return new ResourceNotFoundException("User not found: " + email);
                });

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L));
        token.setRevoked(false);

        RefreshToken saved = refreshTokenRepository.save(token);
        log.info("Refresh token created for user: {} (expires at {})", email, saved.getExpiresAt());
        return saved;
    }

    public RefreshToken verify(String token) {
        log.debug("Verifying refresh token");

        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found in DB");
                    return new ResourceNotFoundException("Refresh token not found");
                });

        if (Boolean.TRUE.equals(rt.getRevoked())) {
            log.warn("Refresh token already revoked for user: {}", rt.getUser().getEmail());
            throw new IllegalStateException("Refresh token has been revoked");
        }

        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Refresh token expired for user: {}", rt.getUser().getEmail());
            throw new IllegalStateException("Refresh token has expired");
        }

        log.debug("Refresh token is valid for user: {}", rt.getUser().getEmail());
        return rt;
    }

    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            log.info("Refresh token revoked for user: {}", rt.getUser().getEmail());
        });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("All refresh tokens revoked for user ID: {}", userId);
    }
}