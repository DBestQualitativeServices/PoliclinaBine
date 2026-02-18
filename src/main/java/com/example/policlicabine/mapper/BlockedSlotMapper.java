package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.BlockedSlotDto;
import com.example.policlicabine.entity.BlockedSlot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BlockedSlotMapper {

    @Mapping(target = "doctorId", source = "doctor.doctorId")
    @Mapping(target = "createdByUserId", source = "createdBy.userId")
    BlockedSlotDto toDto(BlockedSlot slot);
}
