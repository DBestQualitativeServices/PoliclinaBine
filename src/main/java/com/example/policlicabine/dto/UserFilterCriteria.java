package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFilterCriteria {

    private String username;

    private String fullName;

    private UserRole role;

    private Boolean enabled;

    private Boolean accountNonLocked;

    private OffsetDateTime createdAfter;

    private OffsetDateTime createdBefore;
}
