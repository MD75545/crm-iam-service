package com.crm.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crm.entity.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

}
