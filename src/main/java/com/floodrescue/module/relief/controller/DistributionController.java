package com.floodrescue.module.relief.controller;

import com.floodrescue.module.relief.dto.request.DistributionTaskAssignRequest;
import com.floodrescue.module.relief.dto.request.DistributionVoucherCreateRequest;
import com.floodrescue.module.relief.dto.response.DistributionVoucherResponse;
import com.floodrescue.module.relief.service.DistributionService;
import com.floodrescue.shared.enums.InventoryDocumentStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/distributions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class DistributionController {

    private final DistributionService distributionService;

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return Long.parseLong(userDetails.getUsername());
    }

    @PostMapping
    public ResponseEntity<DistributionVoucherResponse> createVoucher(
            @Valid @RequestBody DistributionVoucherCreateRequest request,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(distributionService.createVoucher(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DistributionVoucherResponse> getVoucher(@PathVariable Long id) {
        return ResponseEntity.ok(distributionService.getVoucher(id));
    }

    @GetMapping
    public ResponseEntity<Page<DistributionVoucherResponse>> listVouchers(
            @RequestParam(required = false) InventoryDocumentStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(distributionService.listVouchers(status, pageable));
    }

    @PutMapping("/{id}/assignment")
    public ResponseEntity<DistributionVoucherResponse> upsertAssignment(
            @PathVariable Long id,
            @RequestBody DistributionTaskAssignRequest request,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(distributionService.assignTask(id, userId, request));
    }

    @PatchMapping("/{id}/assignment")
    public ResponseEntity<DistributionVoucherResponse> patchAssignment(
            @PathVariable Long id,
            @RequestBody DistributionTaskAssignRequest request,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(distributionService.assignTask(id, userId, request));
    }
}
