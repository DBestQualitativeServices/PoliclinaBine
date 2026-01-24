package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.SessionBillingDto;
import com.example.policlicabine.entity.SessionBilling;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for SessionBilling entity to SessionBillingDto.
 *
 * Handles conversion between SessionBilling entities and DTOs with proper
 * relationship handling to prevent circular references.
 */
@Mapper(componentModel = "spring", uses = {BillingDiscountMapper.class})
public interface BillingMapper {

    /**
     * Maps SessionBilling entity to SessionBillingDto.
     *
     * @param billing the SessionBilling entity to map
     * @return the mapped SessionBillingDto, or null if input is null
     */
    @Mapping(target = "sessionId", source = "session.sessionId")
    SessionBillingDto toDto(SessionBilling billing);
}
