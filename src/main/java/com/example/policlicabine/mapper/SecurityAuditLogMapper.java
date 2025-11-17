package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.SecurityAuditLogDto;
import com.example.policlicabine.entity.SecurityAuditLog;
import com.example.policlicabine.entity.enums.AuditEventType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper for SecurityAuditLog entity.
 * Handles bidirectional conversion between entity and DTO.
 */
@Mapper(componentModel = "spring")
public interface SecurityAuditLogMapper {

    /**
     * Convert entity to DTO.
     * Maps allowedRoles string to array.
     */
    @Mapping(target = "allowedRoles", source = ".", qualifiedByName = "getAllowedRolesArray")
    SecurityAuditLogDto toDto(SecurityAuditLog entity);

    /**
     * Convert DTO to entity.
     * Maps allowedRoles array to string.
     * Handles frontend "event" field by mapping it to eventType.
     */
    @Mapping(target = "allowedRoles", source = ".", qualifiedByName = "setAllowedRolesFromArray")
    @Mapping(target = "eventType", source = ".", qualifiedByName = "resolveEventType")
    SecurityAuditLog toEntity(SecurityAuditLogDto dto);

    /**
     * Named mapping: Extract allowedRoles array from entity.
     */
    @Named("getAllowedRolesArray")
    default String[] getAllowedRolesArray(SecurityAuditLog entity) {
        return entity.getAllowedRolesArray();
    }

    /**
     * Named mapping: Set allowedRoles string from DTO array.
     */
    @Named("setAllowedRolesFromArray")
    default String setAllowedRolesFromArray(SecurityAuditLogDto dto) {
        String[] roles = dto.getAllowedRoles();
        return roles != null && roles.length > 0 ? String.join(",", roles) : null;
    }

    /**
     * Named mapping: Resolve eventType from DTO.
     * Handles both "eventType" field and "event" field (from frontend).
     */
    @Named("resolveEventType")
    default AuditEventType resolveEventType(SecurityAuditLogDto dto) {
        // If eventType is directly set, use it
        if (dto.getEventType() != null) {
            return dto.getEventType();
        }

        // Otherwise, try to parse from "event" field (frontend compatibility)
        if (dto.getEvent() != null && !dto.getEvent().isEmpty()) {
            try {
                return AuditEventType.valueOf(dto.getEvent());
            } catch (IllegalArgumentException e) {
                // If event string doesn't match enum, default to CUSTOM_EVENT
                return AuditEventType.CUSTOM_EVENT;
            }
        }

        // Default fallback
        return AuditEventType.CUSTOM_EVENT;
    }
}
