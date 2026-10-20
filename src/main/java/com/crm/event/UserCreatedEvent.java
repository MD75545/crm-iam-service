package com.crm.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent {

    private String eventId;           // unique per event – for idempotency
    private String eventType;         // "user.created"
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private UUID tenantId;
    private String role;              // ADMIN, CUSTOMER, etc.
    private LocalDateTime occurredAt;
}