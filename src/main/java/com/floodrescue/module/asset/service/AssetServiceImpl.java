package com.floodrescue.module.asset.service;

import com.floodrescue.module.asset.dto.request.CreateAssetRequest;
import com.floodrescue.module.asset.dto.request.AssetStatusUpdateRequest;
import com.floodrescue.module.asset.dto.response.AssetResponse;
import com.floodrescue.module.asset.entity.AssetEntity;
import com.floodrescue.module.asset.repository.AssetReponsitory;
import com.floodrescue.module.team.entity.TeamEntity;
import com.floodrescue.module.team.repository.TeamRepository;
import com.floodrescue.shared.enums.AssetStatus;
import com.floodrescue.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetReponsitory assetRepository;
    private final TeamRepository teamRepository;

    @Override
    @Transactional
    public AssetResponse createAsset(CreateAssetRequest request) {
        TeamEntity assignedTeam = null;
        if (request.getAssignedTeamId() != null) {
            assignedTeam = teamRepository.findById(request.getAssignedTeamId())
                    .orElseThrow(() -> new NotFoundException("Đội cứu hộ không tồn tại"));
        }

        AssetEntity asset = AssetEntity.builder()
                .code(request.getCode().trim())
                .name(request.getName().trim())
                .assetType(request.getAssetType().trim())
                .capacity(request.getCapacity())
                .status(AssetStatus.AVAILABLE)
                .assignedTeam(assignedTeam)
                .note(request.getNote())
                .build();

        asset = assetRepository.save(asset);
        return toResponse(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponse> getAssets(AssetStatus status) {
        // Fetch join assignedTeam để FE nhìn thấy được assignedTeamId/Name (tránh lazy null)
        return assetRepository.findAllWithTeam(status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponse getAsset(Long id) {
        AssetEntity asset = assetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phương tiện không tồn tại"));
        return toResponse(asset);
    }

    @Override
    @Transactional
    public AssetResponse updateAssetStatus(Long id, AssetStatusUpdateRequest request) {
        AssetEntity asset = assetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phương tiện không tồn tại"));
        asset.setStatus(request.getStatus());
        return toResponse(assetRepository.save(asset));
    }

    private AssetResponse toResponse(AssetEntity a) {
        Long teamId = null;
        String teamName = null;
        if (a.getAssignedTeam() != null) {
            teamId = a.getAssignedTeam().getId();
            teamName = a.getAssignedTeam().getName();
        }

        return AssetResponse.builder()
                .id(a.getId())
                .code(a.getCode())
                .name(a.getName())
                .assetType(a.getAssetType())
                .status(a.getStatus())
                .capacity(a.getCapacity())
                .assignedTeamId(teamId)
                .assignedTeamName(teamName)
                .note(a.getNote())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
