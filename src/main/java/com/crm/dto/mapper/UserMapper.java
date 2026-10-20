package com.crm.dto.mapper;

import com.crm.dto.UserRequest;
import com.crm.dto.UserResponse;
import com.crm.entity.Role;
import com.crm.entity.Tenant;
import com.crm.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    /**
     * Convert Entity → Response DTO.
     * Call this for every API response.
     */
    public UserResponse toResponse(User user) {
        if (user == null) return null;

        UserResponse dto = new UserResponse();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setProfilePicUrl(user.getProfilePicUrl());
        dto.setStatus(user.getStatus());
        dto.setEmailVerified(user.getEmailVerified());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        if (user.getTenant() != null) {
            dto.setTenantId(user.getTenant().getId());
            dto.setTenantName(user.getTenant().getName());
            dto.setTenantCode(user.getTenant().getCode());
        }
        if (user.getRole() != null) {
            dto.setRoleId(user.getRole().getId());
            dto.setRoleName(user.getRole().getName());
        }
        return dto;
    }

    /**
     * Populate an existing entity from a Request DTO.
     * The caller must set tenant, role, and encode the password.
     */
    public void updateEntityFromRequest(User user, UserRequest req) {
        user.setEmail(req.getEmail());
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setPhone(req.getPhone());
        if (req.getStatus() != null) user.setStatus(req.getStatus());
        if (req.getProfilePicUrl() != null) user.setProfilePicUrl(req.getProfilePicUrl());
        // password handled separately in service
    }
}