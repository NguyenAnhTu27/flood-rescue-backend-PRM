package com.floodrescue.module.asset.controller;

import com.floodrescue.module.asset.dto.request.CreateAssetRequest;
import com.floodrescue.module.asset.dto.request.AssetStatusUpdateRequest;
import com.floodrescue.module.asset.dto.response.AssetResponse;
import com.floodrescue.module.asset.service.AssetService;
import com.floodrescue.shared.enums.AssetStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    /**
     * Tạo phương tiện / thiết bị mới (chỉ dành cho Manager).
     */
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<AssetResponse> createAsset(@Valid @RequestBody CreateAssetRequest request) {
        AssetResponse asset = assetService.createAsset(request);
        return ResponseEntity.ok(asset);
    }

    /**
     * Lấy danh sách phương tiện, có thể lọc theo trạng thái.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','COORDINATOR')")
    public ResponseEntity<List<AssetResponse>> getAssets(
            @RequestParam(required = false) AssetStatus status
    ) {
        return ResponseEntity.ok(assetService.getAssets(status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','COORDINATOR')")
    public ResponseEntity<AssetResponse> getAsset(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.getAsset(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<AssetResponse> updateAssetStatus(
            @PathVariable Long id,
            @Valid @RequestBody AssetStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(assetService.updateAssetStatus(id, request));
    }
}
