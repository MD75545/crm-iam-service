package com.crm.service;

import com.crm.dto.UserRequest;
import com.crm.dto.UserResponse;
import com.crm.dto.mapper.UserMapper;
import com.crm.entity.Role;
import com.crm.entity.Tenant;
import com.crm.entity.User;
import com.crm.event.UserCreatedEvent;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.RoleRepository;
import com.crm.repository.TenantRepository;
import com.crm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserMapper userMapper;
    
    @Autowired
    private com.crm.kafka.KafkaProducerService kafkaProducerService;

    // ---------- READ ----------

    public List<UserResponse> findAllAsDto() {
        log.debug("Fetching all users");
        List<UserResponse> users = userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
        log.debug("Found {} users", users.size());
        return users;
    }

    public UserResponse findByIdAsDto(UUID id) {
        log.debug("Fetching user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
        return userMapper.toResponse(user);
    }

    public User findByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });
    }

    // ---------- CREATE ----------

    @Transactional
    public UserResponse createUser(UserRequest req) {
        log.info("Creating new user with email: {}", req.getEmail());

        userRepository.findByEmailAndTenantId(req.getEmail(), req.getTenantId()).ifPresent(u -> {
            log.warn("Duplicate email attempt: {} in tenant {}", req.getEmail(), req.getTenantId());
            throw new IllegalArgumentException(
                    "A user with this email already exists in this tenant");
        });

        Tenant tenant = tenantRepository.findById(req.getTenantId())
                .orElseThrow(() -> {
                    log.warn("Tenant not found: {}", req.getTenantId());
                    return new ResourceNotFoundException("Tenant not found");
                });

        Role role = roleRepository.findById(req.getRoleId())
                .orElseThrow(() -> {
                    log.warn("Role not found: {}", req.getRoleId());
                    return new ResourceNotFoundException("Role not found");
                });

        User user = new User();
        user.setTenant(tenant);
        user.setRole(role);
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setPhone(req.getPhone());
        user.setProfilePicUrl(req.getProfilePicUrl());
        user.setStatus(req.getStatus() != null ? req.getStatus() : "ACTIVE");
        user.setEmailVerified(false);
        user.setFailedLoginAttempts(0);
        user.setCreatedBy(tenant.getId());

        User saved = userRepository.save(user);
     // Publish event AFTER successful save
        UserCreatedEvent event = new UserCreatedEvent(
                java.util.UUID.randomUUID().toString(),
                "user.created",
                saved.getId(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getTenant().getId(),
                saved.getRole().getName(),
                java.time.LocalDateTime.now()
        );
        kafkaProducerService.publishUserCreated(event);


        
        log.info("User created successfully with id: {} (role: {})", saved.getId(), role.getName());
        return userMapper.toResponse(saved);
    }

    // ---------- UPDATE ----------

    @Transactional
    public UserResponse updateUser(UUID id, UserRequest req) {
        log.info("Updating user with id: {}", id);

        User existing = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found for update with id: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });

        if (req.getRoleId() != null && !req.getRoleId().equals(existing.getRole().getId())) {
            Role newRole = roleRepository.findById(req.getRoleId())
                    .orElseThrow(() -> {
                        log.warn("Role not found: {}", req.getRoleId());
                        return new ResourceNotFoundException("Role not found");
                    });
            existing.setRole(newRole);
            log.info("Role changed for user {} to {}", id, newRole.getName());
        }

        userMapper.updateEntityFromRequest(existing, req);

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            existing.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            log.info("Password updated for user: {}", id);
        }

        User updated = userRepository.save(existing);
        log.info("User updated successfully: {}", updated.getId());
        return userMapper.toResponse(updated);
    }

    // ---------- DELETE ----------

    @Transactional
    public void deleteUser(UUID id) {
        log.info("Deleting user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found for delete with id: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
        userRepository.delete(user);
        log.info("User deleted successfully: {}", id);
    }
}