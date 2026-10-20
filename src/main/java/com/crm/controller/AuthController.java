package com.crm.controller;

import com.crm.entity.RefreshToken;
import com.crm.entity.User;
import com.crm.security.JwtUtil;
import com.crm.service.RefreshTokenService;
import com.crm.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private UserService userService;

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        log.info("Login attempt for email: {}", email);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, credentials.get("password"))
        );

        User user = userService.findByEmail(email);   // loads user with tenant + role
        String accessToken = jwtUtil.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(email);

        log.info("Login successful for email: {} (tenant: {}, role: {})",
                email, user.getTenant().getId(), user.getRole().getName());

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken.getToken(),
                "tokenType", "Bearer",
                "expiresIn", "900"
        );
    }

    @PostMapping("/refresh")
    public Map<String, String> refresh(@RequestBody Map<String, String> body) {
        String refreshTokenStr = body.get("refreshToken");
        log.debug("Token refresh requested");

        RefreshToken rt = refreshTokenService.verify(refreshTokenStr);
        User user = userService.findByEmail(rt.getUser().getEmail());

        String newAccess = jwtUtil.generateToken(user);
        log.info("Access token refreshed for user: {}", user.getEmail());
        return Map.of("accessToken", newAccess);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody Map<String, String> body) {
        refreshTokenService.revoke(body.get("refreshToken"));
        log.info("Logout completed");
        return ResponseEntity.noContent().build();
    }
}