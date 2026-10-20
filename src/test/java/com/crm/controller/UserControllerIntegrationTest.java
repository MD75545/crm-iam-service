package com.crm.controller;

import com.crm.BaseIntegrationTest;
import com.crm.dto.UserRequest;
import com.crm.entity.Role;
import com.crm.entity.Tenant;
import com.crm.repository.RefreshTokenRepository;
import com.crm.repository.RoleRepository;
import com.crm.repository.TenantRepository;
import com.crm.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    private Tenant testTenant;
    private Role adminRole;
    private Role customerRole;

    @BeforeEach
    void setup() {
    	refreshTokenRepository.deleteAll();   // ← delete refresh tokens first
        userRepository.deleteAll();
        roleRepository.deleteAll();
        tenantRepository.deleteAll();

        // Create a tenant
        testTenant = new Tenant();
        testTenant.setName("Test Tenant");
        testTenant.setCode("TEST");
        testTenant.setStatus("ACTIVE");
        testTenant = tenantRepository.save(testTenant);

        // Create roles
        adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setTenant(testTenant);
        adminRole = roleRepository.save(adminRole);

        customerRole = new Role();
        customerRole.setName("CUSTOMER");
        customerRole.setTenant(testTenant);
        customerRole = roleRepository.save(customerRole);
    }

    // ---------------- CREATE ----------------

    @Test
    @Order(1)
    @DisplayName("POST /api/users – should create user as ADMIN → 201 Created")
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void createUser_asAdmin_returns201() throws Exception {
        // Arrange
        UserRequest req = new UserRequest();
        req.setTenantId(testTenant.getId());
        req.setRoleId(customerRole.getId());
        req.setEmail("customer1@test.com");
        req.setPassword("Customer@123");
        req.setFirstName("Customer");
        req.setLastName("One");
        req.setPhone("9876543210");
        req.setStatus("ACTIVE");

        // Act + Assert
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("customer1@test.com"))
                .andExpect(jsonPath("$.roleName").value("CUSTOMER"))
                .andExpect(jsonPath("$.tenantName").value("Test Tenant"))
                // Assert passwordHash NOT returned
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // Assert DB side effect
        Assertions.assertEquals(1, userRepository.count());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/users – should return 403 when CUSTOMER tries")
    @WithMockUser(username = "customer@test.com", roles = {"CUSTOMER"})
    void createUser_asCustomer_returns403() throws Exception {
        UserRequest req = new UserRequest();
        req.setTenantId(testTenant.getId());
        req.setRoleId(customerRole.getId());
        req.setEmail("hacker@test.com");
        req.setPassword("Hacker@123");
        req.setFirstName("Hacker");
        req.setLastName("User");

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        Assertions.assertEquals(0, userRepository.count());
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/users – should return 400 for invalid email")
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void createUser_invalidEmail_returns400() throws Exception {
        UserRequest req = new UserRequest();
        req.setTenantId(testTenant.getId());
        req.setRoleId(customerRole.getId());
        req.setEmail("not-an-email");   // invalid
        req.setPassword("Password@123");
        req.setFirstName("Test");
        req.setLastName("User");

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    // ---------------- READ ----------------

    @Test
    @Order(4)
    @DisplayName("GET /api/users – should return 200 for ADMIN")
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void getAllUsers_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/users – should return 403 for CUSTOMER")
    @WithMockUser(username = "customer@test.com", roles = {"CUSTOMER"})
    void getAllUsers_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/users/{id} – should return 404 for unknown UUID")
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void getUserById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/users/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // ---------------- UPDATE ----------------

    @Test
    @Order(7)
    @DisplayName("PUT /api/users/{id} – should update user as ADMIN")
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void updateUser_asAdmin_returns200() throws Exception {
        // Arrange – create a user first
        UserRequest createReq = new UserRequest();
        createReq.setTenantId(testTenant.getId());
        createReq.setRoleId(customerRole.getId());
        createReq.setEmail("update@test.com");
        createReq.setPassword("Update@123");
        createReq.setFirstName("Update");
        createReq.setLastName("Me");
        createReq.setStatus("ACTIVE");

        String createResp = mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(createResp).get("id").asText();

        // Update payload
        UserRequest updateReq = new UserRequest();
        updateReq.setTenantId(testTenant.getId());
        updateReq.setRoleId(customerRole.getId());
        updateReq.setEmail("update@test.com");
        updateReq.setFirstName("Updated");
        updateReq.setLastName("Name");
        updateReq.setStatus("INACTIVE");

        mockMvc.perform(put("/api/users/" + userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    // ---------------- DELETE ----------------

    @Test
    @Order(8)
    @DisplayName("DELETE /api/users/{id} – should delete user as ADMIN → 204")
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void deleteUser_asAdmin_returns204() throws Exception {
        // Create a user
        UserRequest req = new UserRequest();
        req.setTenantId(testTenant.getId());
        req.setRoleId(customerRole.getId());
        req.setEmail("delete@test.com");
        req.setPassword("Delete@123");
        req.setFirstName("Delete");
        req.setLastName("Me");

        String resp = mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(resp).get("id").asText();

        // Delete
        mockMvc.perform(delete("/api/users/" + userId).with(csrf()))
                .andExpect(status().isNoContent());

        // Verify DB
        Assertions.assertFalse(userRepository.findById(java.util.UUID.fromString(userId)).isPresent());
    }

    @Test
    @Order(9)
    @DisplayName("DELETE /api/users/{id} – 404 for unknown user")
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void deleteUser_unknownId_returns404() throws Exception {
        mockMvc.perform(delete("/api/users/00000000-0000-0000-0000-000000000000").with(csrf()))
                .andExpect(status().isNotFound());
    }
}