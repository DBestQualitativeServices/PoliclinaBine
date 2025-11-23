package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.FileDto;
import com.example.policlicabine.entity.enums.FileCategory;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaTypeFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "File Management")
public class FileController {

    private final FileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @StandardApiResponses
    @Operation(summary = "Upload a file")
    public FileDto uploadFile(
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam("category") @NotNull FileCategory category,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validUntil,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String username = userDetails.getUsername();
        log.info("File upload request: {} ({} bytes) by user: {}",
                file.getOriginalFilename(), file.getSize(), username);

        Result<FileDto> result = fileService.uploadFile(
                file, category, username, validFrom, validUntil
        );

        if (result.isFailure()) {
            log.warn("File upload failed: {}", result.getErrorMessage());
            throw new BusinessException(result.getErrorMessage());
        }

        log.info("File uploaded successfully: {}", result.getValue().getId());
        return result.getValue();
    }

    @PostMapping("/{previousFileId}/new-version")
    @StandardApiResponses
    @Operation(summary = "Upload new version of existing file")
    public FileDto uploadNewVersion(
            @PathVariable UUID previousFileId,
            @RequestParam("file") @NotNull MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String username = userDetails.getUsername();
        log.info("New version upload request for file: {} by user: {}",
                previousFileId, username);

        Result<FileDto> result = fileService.uploadNewVersion(
                previousFileId, file, username
        );

        if (result.isFailure()) {
            throw new ResourceNotFoundException("File", previousFileId);
        }

        return result.getValue();
    }

    @GetMapping("/{fileId}/download")
    @StandardApiResponses
    @Operation(summary = "Download a file")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID fileId) {
        log.debug("File download request: {}", fileId);

        Result<FileDto> metadataResult = fileService.findById(fileId);
        if (metadataResult.isFailure()) {
            throw new ResourceNotFoundException("File", fileId);
        }

        FileDto fileDto = metadataResult.getValue();

        Result<Resource> result = fileService.downloadFile(fileId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("File", fileId);
        }

        Resource resource = result.getValue();

        ContentDisposition contentDisposition = ContentDisposition
                .attachment()
                .filename(fileDto.getOriginalFilename(), StandardCharsets.UTF_8)
                .build();

        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        log.info("File downloaded: {}", fileId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(mediaType)
                .body(resource);
    }

    @GetMapping("/{fileId}")
    @StandardApiResponses
    @Operation(summary = "Get file metadata")
    public FileDto getFileMetadata(@PathVariable UUID fileId) {
        Result<FileDto> result = fileService.findById(fileId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("File", fileId);
        }

        return result.getValue();
    }

    @GetMapping
    @StandardApiResponses
    @Operation(summary = "List files by category")
    public List<FileDto> listFilesByCategory(@RequestParam FileCategory category) {
        Result<List<FileDto>> result = fileService.findByCategory(category);

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }

    @GetMapping("/{fileId}/versions")
    @StandardApiResponses
    @Operation(summary = "Get file version history")
    public List<FileDto> getFileVersionHistory(@PathVariable UUID fileId) {
        Result<List<FileDto>> result = fileService.getFileVersionHistory(fileId);

        if (result.isFailure()) {
            throw new ResourceNotFoundException("File", fileId);
        }

        return result.getValue();
    }

    @DeleteMapping("/{fileId}")
    @StandardApiResponses
    @Operation(summary = "Soft delete a file")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String username = userDetails.getUsername();
        log.info("File deletion request: {} by user: {}", fileId, username);

        Result<Void> result = fileService.softDeleteFile(fileId, username);

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/expired")
    @StandardApiResponses
    @Operation(summary = "List expired files")
    public List<FileDto> listExpiredFiles() {
        Result<List<FileDto>> result = fileService.findExpiredFiles();

        if (result.isFailure()) {
            throw new BusinessException(result.getErrorMessage());
        }

        return result.getValue();
    }
}
