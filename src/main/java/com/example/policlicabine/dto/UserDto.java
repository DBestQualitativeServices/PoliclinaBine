package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User account information with roles")
public class UserDto {

    private UUID userId;
    private String username;
    private Set<UserRole> roles;
}
