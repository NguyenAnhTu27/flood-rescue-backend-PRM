package com.floodrescue.module.asset.controller;

import com.floodrescue.module.asset.dto.request.AssignTeamRequest;
import com.floodrescue.module.asset.dto.request.ChangeAssetStatusRequest;
import com.floodrescue.module.asset.dto.request.CreateAssetRequest;
import com.floodrescue.module.asset.dto.request.UpdateAssetRequest;
import com.floodrescue.module.asset.dto.response.AssetResponse;
import com.floodrescue.module.asset.service.AssetService;
import com.floodrescue.shared.enums.AssetStatus;
import com.floodrescue.shared.enums.AssetType;
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

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<AssetResponse> createAsset(@Valid @RequestBody CreateAssetRequest request) {
        return ResponseEntity.ok(assetService.createAsset(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','COORDINATOR')")
    public ResponseEntity<AssetResponse> getAssetById(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.getAssetById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<AssetResponse> updateAsset(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssetRequest request) {
        return ResponseEntity.ok(assetService.updateAsset(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER','COORDINATOR')")
    public ResponseEntity<AssetResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeAssetStatusRequest request) {
        return ResponseEntity.ok(assetService.changeStatus(id, request.getNewStatus()));
    }

    @PutMapping("/{id}/assign-team")
    @PreAuthorize("hasAnyRole('MANAGER','COORDINATOR')")
    public ResponseEntity<AssetResponse> assignTeam(
            @PathVariable Long id,
            @Valid @RequestBody AssignTeamRequest request) {
        return ResponseEntity.ok(assetService.assignTeam(id, request.getTeamId()));
    }

    @PutMapping("/{id}/unassign-team")
    @PreAuthorize("hasAnyRole('MANAGER','COORDINATOR')")
    public ResponseEntity<AssetResponse> unassignTeam(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.unassignTeam(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','COORDINATOR')")
    public ResponseEntity<List<AssetResponse>> getAssets(
            @RequestParam(required = false) AssetStatus status) {
        return ResponseEntity.ok(assetService.getAssets(status));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','COORDINATOR')")
    public ResponseEntity<List<AssetResponse>> getAssetsFiltered(
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) AssetType assetType,
            @RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(assetService.getAssetsFiltered(status, assetType, teamId));
    }

    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','COORDINATOR','RESCUER')")
    public ResponseEntity<List<AssetResponse>> getAssetsByTeam(@PathVariable Long teamId) {
        return ResponseEntity.ok(assetService.getAssetsByTeam(teamId));
    }
}
