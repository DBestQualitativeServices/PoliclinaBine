package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ErrorResponse;
import com.example.policlicabine.dto.FileDto;
import com.example.policlicabine.entity.FileCategory;
import com.example.policlicabine.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaTypeFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for file management operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>File upload with metadata</li>
 *   <li>File download with proper content headers</li>
 *   <li>File metadata retrieval</li>
 *   <li>Version management</li>
 *   <li>File deletion</li>
 * </ul>
 *
 * @author PoliclicaBine System
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "File Management", description = "File upload, download, and management operations")
public class FileController {

    private final FileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a file",
            description = "Upload a new file with category and validity metadata. " +
                    "Maximum file size: 25MB. Allowed types: PNG, JPEG, JPG images."
    )
    @ApiResponse(responseCode = "200", description = "File uploaded successfully",
            content = @Content(schema = @Schema(implementation = FileDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid file or parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "File too large",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<?> uploadFile(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") @NotNull MultipartFile file,

            @Parameter(description = "File category", required = true)
            @RequestParam("category") @NotNull FileCategory category,

            @Parameter(description = "UUID of user uploading the file", required = true)
            @RequestParam("uploadedByUserId") @NotNull UUID uploadedByUserId,

            @Parameter(description = "Start date of file validity (ISO date format: YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validFrom,

            @Parameter(description = "End date of file validity (ISO date format: YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validUntil,

            HttpServletRequest request
    ) {
        log.info("File upload request: {} ({} bytes) by user: {}",
                file.getOriginalFilename(), file.getSize(), uploadedByUserId);

        Result<FileDto> result = fileService.uploadFile(
                file, category, uploadedByUserId, validFrom, validUntil
        );

        if (result.isFailure()) {
            log.warn("File upload failed: {}", result.getErrorMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
                            result.getErrorMessage(), request.getRequestURI()));
        }

        log.info("File uploaded successfully: {}", result.getValue().getId());
        return ResponseEntity.ok(result.getValue());
    }

    @PostMapping("/{previousFileId}/new-version")
    @Operation(
            summary = "Upload new version of existing file",
            description = "Upload a new version of an existing file. " +
                    "The previous version will be soft-deleted and linked to the new version."
    )
    @ApiResponse(responseCode = "200", description = "New version uploaded successfully")
    @ApiResponse(responseCode = "404", description = "Previous file not found")
    public ResponseEntity<?> uploadNewVersion(
            @Parameter(description = "UUID of the file to replace")
            @PathVariable UUID previousFileId,

            @Parameter(description = "New file version")
            @RequestParam("file") @NotNull MultipartFile file,

            @Parameter(description = "UUID of user uploading")
            @RequestParam("uploadedByUserId") @NotNull UUID uploadedByUserId,

            HttpServletRequest request
    ) {
        log.info("New version upload request for file: {} by user: {}",
                previousFileId, uploadedByUserId);

        Result<FileDto> result = fileService.uploadNewVersion(
                previousFileId, file, uploadedByUserId
        );

        if (result.isFailure()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(),
                            result.getErrorMessage(), request.getRequestURI()));
        }

        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping("/{fileId}/download")
    @Operation(
            summary = "Download a file",
            description = "Download file content with proper content-disposition headers. " +
                    "Returns 404 if file not found or expired."
    )
    @ApiResponse(responseCode = "200", description = "File downloaded successfully",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE))
    @ApiResponse(responseCode = "404", description = "File not found or expired")
    public ResponseEntity<?> downloadFile(
            @Parameter(description = "UUID of the file to download")
            @PathVariable UUID fileId,

            HttpServletRequest request
    ) {
        log.debug("File download request: {}", fileId);

        // Get file metadata first
        Result<FileDto> metadataResult = fileService.findById(fileId);
        if (metadataResult.isFailure()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(),
                            metadataResult.getErrorMessage(), request.getRequestURI()));
        }

        FileDto fileDto = metadataResult.getValue();

        // Get file resource
        Result<Resource> result = fileService.downloadFile(fileId);

        if (result.isFailure()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(),
                            result.getErrorMessage(), request.getRequestURI()));
        }

        Resource resource = result.getValue();

        // Build Content-Disposition header
        ContentDisposition contentDisposition = ContentDisposition
                .attachment()
                .filename(fileDto.getOriginalFilename(), StandardCharsets.UTF_8)
                .build();

        // Detect MIME type
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        log.info("File downloaded: {}", fileId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(mediaType)
                .body(resource);
    }

    @GetMapping("/{fileId}")
    @Operation(
            summary = "Get file metadata",
            description = "Retrieve metadata and information about a file without downloading content"
    )
    @ApiResponse(responseCode = "200", description = "File metadata retrieved successfully")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<?> getFileMetadata(
            @Parameter(description = "UUID of the file")
            @PathVariable UUID fileId,

            HttpServletRequest request
    ) {
        Result<FileDto> result = fileService.findById(fileId);

        if (result.isFailure()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(),
                            result.getErrorMessage(), request.getRequestURI()));
        }

        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping
    @Operation(
            summary = "List files by category",
            description = "Retrieve all active files in a specific category"
    )
    @ApiResponse(responseCode = "200", description = "Files retrieved successfully")
    public ResponseEntity<?> listFilesByCategory(
            @Parameter(description = "File category to filter by")
            @RequestParam FileCategory category
    ) {
        Result<List<FileDto>> result = fileService.findByCategory(category);

        if (result.isFailure()) {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }

        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping("/{fileId}/versions")
    @Operation(
            summary = "Get file version history",
            description = "Retrieve all versions of a file including older and newer versions"
    )
    @ApiResponse(responseCode = "200", description = "Version history retrieved successfully")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<?> getFileVersionHistory(
            @Parameter(description = "UUID of the file")
            @PathVariable UUID fileId,

            HttpServletRequest request
    ) {
        Result<List<FileDto>> result = fileService.getFileVersionHistory(fileId);

        if (result.isFailure()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(),
                            result.getErrorMessage(), request.getRequestURI()));
        }

        return ResponseEntity.ok(result.getValue());
    }

    @DeleteMapping("/{fileId}")
    @Operation(
            summary = "Soft delete a file",
            description = "Mark file as deleted (soft delete). Physical file remains for audit purposes."
    )
    @ApiResponse(responseCode = "204", description = "File deleted successfully")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<?> deleteFile(
            @Parameter(description = "UUID of the file to delete")
            @PathVariable UUID fileId,

            @Parameter(description = "UUID of user performing deletion")
            @RequestParam UUID deletedByUserId,

            HttpServletRequest request
    ) {
        log.info("File deletion request: {} by user: {}", fileId, deletedByUserId);

        Result<Void> result = fileService.softDeleteFile(fileId, deletedByUserId);

        if (result.isFailure()) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
                            result.getErrorMessage(), request.getRequestURI()));
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/expired")
    @Operation(
            summary = "List expired files",
            description = "Retrieve all files that have passed their validity end date"
    )
    @ApiResponse(responseCode = "200", description = "Expired files retrieved successfully")
    public ResponseEntity<?> listExpiredFiles() {
        Result<List<FileDto>> result = fileService.findExpiredFiles();

        if (result.isFailure()) {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }

        return ResponseEntity.ok(result.getValue());
    }
}
