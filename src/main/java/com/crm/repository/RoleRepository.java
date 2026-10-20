package com.crm.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crm.entity.Role;

public interface RoleRepository extends JpaRepository<Role, UUID> {

}
