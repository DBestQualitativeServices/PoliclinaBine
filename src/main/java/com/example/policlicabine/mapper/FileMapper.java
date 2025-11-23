package com.example.policlicabine.mapper;

import com.example.policlicabine.dto.FileDto;
import com.example.policlicabine.entity.File;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.text.DecimalFormat;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface FileMapper {

    @Mapping(target = "downloadUrl", expression = "java(buildDownloadUrl(file))")
    @Mapping(target = "isExpired", expression = "java(file.isExpired())")
    @Mapping(target = "isActive", expression = "java(file.isActive())")
    @Mapping(target = "fileSizeFormatted", expression = "java(formatFileSize(file.getFileSize()))")
    @Mapping(target = "patientId", expression = "java(file.getPatient() != null ? file.getPatient().getPatientId() : null)")
    FileDto toDto(File file);

    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "uploadedBy", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    File toEntity(FileDto dto);

    default String buildDownloadUrl(File file) {
        if (file == null || file.getId() == null) {
            return null;
        }
        return "/api/files/" + file.getId() + "/download";
    }

    default String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) {
            return "0 B";
        }

        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));

        if (digitGroups >= units.length) {
            digitGroups = units.length - 1;
        }

        double size = bytes / Math.pow(1024, digitGroups);
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.#");

        return decimalFormat.format(size) + " " + units[digitGroups];
    }
}
