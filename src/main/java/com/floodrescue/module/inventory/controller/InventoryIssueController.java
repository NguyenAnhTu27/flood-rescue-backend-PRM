package com.floodrescue.module.inventory.controller;

import com.floodrescue.module.inventory.dto.request.InventoryIssueCreateRequest;
import com.floodrescue.module.inventory.dto.response.InventoryIssueResponse;
import com.floodrescue.module.inventory.service.IssueService;
import com.floodrescue.shared.enums.InventoryDocumentStatus;
import com.floodrescue.shared.util.CodeGenerator;
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

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/issues")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class InventoryIssueController {

    private final IssueService issueService;

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return Long.parseLong(userDetails.getUsername());
    }

    @PostMapping
    public ResponseEntity<InventoryIssueResponse> createIssue(
            @Valid @RequestBody InventoryIssueCreateRequest request,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(issueService.createIssue(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryIssueResponse> getIssue(@PathVariable Long id) {
        return ResponseEntity.ok(issueService.getIssue(id));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<InventoryIssueResponse> approveIssue(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(issueService.approveIssue(id, userId));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<InventoryIssueResponse> cancelIssue(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(issueService.cancelIssue(id, userId));
    }

    @PutMapping("/{id}/temporary-deduction")
    public ResponseEntity<InventoryIssueResponse> markTemporaryDeduction(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(issueService.markIssueTemporaryDeduction(id, userId));
    }

    @PutMapping("/{id}/finalize-deduction")
    public ResponseEntity<InventoryIssueResponse> finalizeDeduction(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(issueService.finalizeIssueDeduction(id, userId));
    }

    @GetMapping
    public ResponseEntity<Page<InventoryIssueResponse>> listIssues(
            @RequestParam(required = false) InventoryDocumentStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(issueService.listIssues(status, pageable));
    }

    @GetMapping("/temporary")
    public ResponseEntity<List<InventoryIssueResponse>> listTemporaryIssues() {
        return ResponseEntity.ok(issueService.listIssuesByStatus(InventoryDocumentStatus.APPROVED));
    }

    @GetMapping("/generate-code")
    public ResponseEntity<Map<String, String>> generateIssueCode() {
        return ResponseEntity.ok(Map.of("code", CodeGenerator.generateInventoryIssueCode()));
    }
}
