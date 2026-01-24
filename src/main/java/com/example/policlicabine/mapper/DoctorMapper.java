package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.DoctorDto;
import com.example.policlicabine.entity.Doctor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {WeeklyAvailabilityMapper.class})
public interface DoctorMapper {

    // userId ignored to avoid N+1 from User's bidirectional OneToOne relationships
    @Mapping(target = "userId", ignore = true)
    DoctorDto toDto(Doctor doctor);
}
