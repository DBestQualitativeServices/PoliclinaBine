package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.FormSignatureDto;
import com.example.policlicabine.entity.FormSignature;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Base64;
import java.util.List;

@Mapper(componentModel = "spring")
public interface FormSignatureMapper {

    @Mapping(target = "formSubmissionId", source = "formSubmission.id")
    @Mapping(target = "signedByUserId", source = "signedBy.userId")
    @Mapping(target = "signedByUserName", source = "signedBy.username")
    @Mapping(target = "signatureData", expression = "java(bytesToBase64(entity.getSignatureData()))")
    FormSignatureDto toDto(FormSignature entity);

    List<FormSignatureDto> toDtoList(List<FormSignature> entities);

    @Mapping(target = "formSubmission", ignore = true)
    @Mapping(target = "signedBy", ignore = true)
    @Mapping(target = "signatureData", expression = "java(base64ToBytes(dto.getSignatureData()))")
    FormSignature toEntity(FormSignatureDto dto);

    /**
     * Converts byte array to Base64 string for DTO response.
     */
    default String bytesToBase64(byte[] data) {
        return data != null ? Base64.getEncoder().encodeToString(data) : null;
    }

    /**
     * Converts Base64 string to byte array for entity storage.
     */
    default byte[] base64ToBytes(String base64) {
        return base64 != null ? Base64.getDecoder().decode(base64) : null;
    }
}
