package com.crm.controller;

import com.crm.BaseIntegrationTest;
import com.crm.entity.Role;
import com.crm.entity.Tenant;
import com.crm.entity.User;
import com.crm.repository.RefreshTokenRepository;
import com.crm.repository.RoleRepository;
import com.crm.repository.TenantRepository;
import com.crm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    private User testUser;

    @BeforeEach
    void setup() {
    	refreshTokenRepository.deleteAll();   // ← delete refresh tokens first
        userRepository.deleteAll();
        roleRepository.deleteAll();
        tenantRepository.deleteAll();

        Tenant tenant = new Tenant();
        tenant.setName("Auth Tenant");
        tenant.setCode("AUTH");
        tenant.setStatus("ACTIVE");
        tenant = tenantRepository.save(tenant);

        Role role = new Role();
        role.setName("ADMIN");
        role.setTenant(tenant);
        role = roleRepository.save(role);

        testUser = new User();
        testUser.setTenant(tenant);
        testUser.setRole(role);
        testUser.setEmail("auth@test.com");
        testUser.setPasswordHash(passwordEncoder.encode("Correct@123"));
        testUser.setFirstName("Auth");
        testUser.setLastName("Test");
        testUser.setStatus("ACTIVE");
        testUser.setEmailVerified(true);
        testUser.setFailedLoginAttempts(0);
        testUser.setCreatedBy(tenant.getId());
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("POST /auth/login – correct credentials → 200 + tokens")
    void login_validCredentials_returnsTokens() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "auth@test.com",
                "password", "Correct@123"
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/login – wrong password → 401")
    void login_wrongPassword_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "auth@test.com",
                "password", "WrongPassword"
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login – unknown user → 401")
    void login_unknownUser_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "unknown@test.com",
                "password", "Whatever@123"
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/refresh – valid refresh token → new access token")
    void refresh_validToken_returnsNewAccess() throws Exception {
        // First login
        String loginBody = objectMapper.writeValueAsString(Map.of(
                "email", "auth@test.com",
                "password", "Correct@123"
        ));
        String loginResp = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(loginResp).get("refreshToken").asText();

        // Then refresh
        String refreshBody = objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("POST /auth/refresh – invalid refresh token → 404")
    void refresh_invalidToken_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("refreshToken", "fake-token-abc"));
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /auth/logout – revokes refresh token → 204")
    void logout_validToken_returns204() throws Exception {
        // Login
        String loginBody = objectMapper.writeValueAsString(Map.of(
                "email", "auth@test.com",
                "password", "Correct@123"
        ));
        String loginResp = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andReturn().getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(loginResp).get("refreshToken").asText();

        // Logout
        String logoutBody = objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody))
                .andExpect(status().isNoContent());

        // Attempt refresh after logout → should fail
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody))
                .andExpect(status().is5xxServerError()); // because revoke throws IllegalStateException
    }
}