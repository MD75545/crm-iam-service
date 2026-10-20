package com.crm.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String profilePicUrl;
    private String status;
    private Boolean emailVerified;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Flattened tenant (no full object)
    private UUID tenantId;
    private String tenantName;
    private String tenantCode;

    // Flattened role (no full object)
    private UUID roleId;
    private String roleName;
}