package com.floodrescue.module.rescue.controller;

import com.floodrescue.module.rescue.dto.request.AddNoteRequest;
import com.floodrescue.module.rescue.dto.request.ConfirmRescueResultRequest;
import com.floodrescue.module.rescue.dto.request.RescueRequestCreateRequest;
import com.floodrescue.module.rescue.dto.request.ReopenCancelledRequest;
import com.floodrescue.module.rescue.dto.request.RescueRequestUpdateRequest;
import com.floodrescue.module.rescue.dto.response.AttachmentUploadResponse;
import com.floodrescue.module.rescue.dto.response.CitizenRescueConfirmationResponse;
import com.floodrescue.module.rescue.dto.response.RescueRequestResponse;
import com.floodrescue.shared.enums.AttachmentFileType;
import com.floodrescue.module.rescue.service.RescueRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rescue/citizen")
@RequiredArgsConstructor
public class CitizenRescueController {

    private final RescueRequestService rescueRequestService;

    @Value("${app.upload.rescue-dir:uploads/rescue}")
    private String rescueUploadDir;

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return Long.parseLong(userDetails.getUsername());
    }

    @PostMapping("/requests")
    public ResponseEntity<RescueRequestResponse> createRescueRequest(
            @Valid @RequestBody RescueRequestCreateRequest request,
            Authentication authentication) {
        Long citizenId = getCurrentUserId(authentication);
        RescueRequestResponse response = rescueRequestService.createRescueRequest(citizenId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/requests")
    public ResponseEntity<Page<RescueRequestResponse>> getMyRescueRequests(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        Long citizenId = getCurrentUserId(authentication);
        Page<RescueRequestResponse> response = rescueRequestService.getRescueRequestsByCitizen(citizenId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<RescueRequestResponse> getRescueRequestById(@PathVariable Long id) {
        RescueRequestResponse response = rescueRequestService.getRescueRequestById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/requests/{id}")
    public ResponseEntity<RescueRequestResponse> updateRescueRequest(
            @PathVariable Long id,
            @Valid @RequestBody RescueRequestUpdateRequest request,
            Authentication authentication) {
        Long citizenId = getCurrentUserId(authentication);
        RescueRequestResponse response = rescueRequestService.updateRescueRequest(id, citizenId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/requests/{id}")
    public ResponseEntity<Map<String, String>> cancelRescueRequest(
            @PathVariable Long id,
            Authentication authentication) {
        Long citizenId = getCurrentUserId(authentication);
        rescueRequestService.cancelRescueRequest(id, citizenId);
        return ResponseEntity.ok(Map.of("message", "Yêu cầu cứu hộ đã được hủy"));
    }

    @PostMapping("/requests/{id}/notes")
    public ResponseEntity<RescueRequestResponse> addNote(
            @PathVariable Long id,
            @Valid @RequestBody AddNoteRequest request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        RescueRequestResponse response = rescueRequestService.addNote(id, userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/requests/{id}/confirm-result")
    public ResponseEntity<CitizenRescueConfirmationResponse> confirmRescueResult(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmRescueResultRequest request,
            Authentication authentication
    ) {
        Long citizenId = getCurrentUserId(authentication);
        CitizenRescueConfirmationResponse response = rescueRequestService.confirmRescueResult(
                id,
                citizenId,
                request.getRescued(),
                request.getReason()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/requests/{id}/reopen")
    public ResponseEntity<RescueRequestResponse> reopenCancelledRequest(
            @PathVariable Long id,
            @Valid @RequestBody ReopenCancelledRequest request,
            Authentication authentication
    ) {
        Long citizenId = getCurrentUserId(authentication);
        return ResponseEntity.ok(rescueRequestService.reopenCancelledRequest(id, citizenId, request.getReason()));
    }

    /**
     * Upload one or more image files for rescue request attachments.
     * Returns list of fileUrl + fileType to be sent back in RescueRequestCreateRequest.attachments.
     */
    @PostMapping("/attachments")
    public ResponseEntity<List<AttachmentUploadResponse>> uploadAttachments(
            @RequestParam("files") List<MultipartFile> files
    ) throws IOException {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Path uploadPath = Paths.get(rescueUploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        List<AttachmentUploadResponse> result = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }

            String newFileName = UUID.randomUUID() + ext;
            Path target = uploadPath.resolve(newFileName);

            // Use Path-based transfer to avoid Tomcat writing into its temp dir
            // and to ensure parent directories are respected
            file.transferTo(target);

            // File will be accessible via /uploads/rescue/{newFileName}
            // Ensure fileUrl starts with / and uses forward slashes
            String fileUrl = "/uploads/rescue/" + newFileName;
            // Normalize to ensure consistent format (remove any double slashes, etc.)
            fileUrl = fileUrl.replaceAll("/+", "/");

            result.add(AttachmentUploadResponse.builder()
                    .fileUrl(fileUrl)
                    .fileType(AttachmentFileType.IMAGE)
                    .build());
        }

        return ResponseEntity.ok(result);
    }
}
