package com.floodrescue.module.inventory.controller;

import com.floodrescue.module.inventory.dto.request.InventoryReceiptCreateRequest;
import com.floodrescue.module.inventory.dto.response.InventoryReceiptResponse;
import com.floodrescue.module.inventory.service.ReceiptService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/receipts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class InventoryReceiptController {

    private final ReceiptService receiptService;

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return Long.parseLong(userDetails.getUsername());
    }

    @PostMapping
    public ResponseEntity<InventoryReceiptResponse> createReceipt(
            @Valid @RequestBody InventoryReceiptCreateRequest request,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(receiptService.createReceipt(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryReceiptResponse> getReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(receiptService.getReceipt(id));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<InventoryReceiptResponse> approveReceipt(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(receiptService.approveReceipt(id, userId));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<InventoryReceiptResponse> cancelReceipt(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(receiptService.cancelReceipt(id, userId));
    }

    @GetMapping
    public ResponseEntity<Page<InventoryReceiptResponse>> listReceipts(
            @RequestParam(required = false) InventoryDocumentStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(receiptService.listReceipts(status, pageable));
    }
}
