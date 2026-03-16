package com.floodrescue.module.asset.service;

import com.floodrescue.module.asset.dto.request.CreateAssetRequest;
import com.floodrescue.module.asset.dto.request.UpdateAssetRequest;
import com.floodrescue.module.asset.dto.response.AssetResponse;
import com.floodrescue.module.asset.entity.AssetEntity;
import com.floodrescue.module.asset.repository.AssetRepository;
import com.floodrescue.module.team.entity.TeamEntity;
import com.floodrescue.module.team.repository.TeamRepository;
import com.floodrescue.shared.enums.AssetStatus;
import com.floodrescue.shared.enums.AssetType;
import com.floodrescue.shared.exception.BusinessException;
import com.floodrescue.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final TeamRepository teamRepository;

    private static final java.util.Map<AssetStatus, Set<AssetStatus>> VALID_TRANSITIONS = java.util.Map.of(
            AssetStatus.AVAILABLE, Set.of(AssetStatus.IN_USE, AssetStatus.MAINTENANCE, AssetStatus.INACTIVE),
            AssetStatus.IN_USE, Set.of(AssetStatus.AVAILABLE, AssetStatus.MAINTENANCE, AssetStatus.BROKEN, AssetStatus.INACTIVE),
            AssetStatus.MAINTENANCE, Set.of(AssetStatus.AVAILABLE, AssetStatus.BROKEN, AssetStatus.INACTIVE),
            AssetStatus.BROKEN, Set.of(AssetStatus.MAINTENANCE, AssetStatus.INACTIVE),
            AssetStatus.INACTIVE, Set.of(AssetStatus.AVAILABLE)
    );

    @Override
    @Transactional
    public AssetResponse createAsset(CreateAssetRequest request) {
        String code = request.getCode().trim();
        if (assetRepository.existsByCode(code)) {
            throw new BusinessException("Mã phương tiện đã tồn tại: " + code);
        }

        TeamEntity assignedTeam = null;
        AssetStatus initialStatus = AssetStatus.AVAILABLE;
        if (request.getAssignedTeamId() != null) {
            assignedTeam = teamRepository.findById(request.getAssignedTeamId())
                    .orElseThrow(() -> new NotFoundException("Đội cứu hộ không tồn tại"));
            if (assignedTeam.getStatus() == null || assignedTeam.getStatus() != 1) {
                throw new BusinessException("Không thể gán phương tiện cho đội không hoạt động");
            }
            initialStatus = AssetStatus.IN_USE;
        }

        AssetEntity asset = AssetEntity.builder()
                .code(code)
                .name(request.getName().trim())
                .assetType(request.getAssetType())
                .capacity(request.getCapacity())
                .status(initialStatus)
                .assignedTeam(assignedTeam)
                .note(request.getNote())
                .build();

        asset = assetRepository.save(asset);
        return toResponse(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponse getAssetById(Long id) {
        AssetEntity asset = assetRepository.findByIdWithTeam(id)
                .orElseThrow(() -> new NotFoundException("Phương tiện không tồn tại"));
        return toResponse(asset);
    }

    @Override
    @Transactional
    public AssetResponse updateAsset(Long id, UpdateAssetRequest request) {
        AssetEntity asset = assetRepository.findByIdWithTeam(id)
                .orElseThrow(() -> new NotFoundException("Phương tiện không tồn tại"));

        if (request.getName() != null) {
            asset.setName(request.getName().trim());
        }
        if (request.getAssetType() != null) {
            asset.setAssetType(request.getAssetType());
        }
        if (request.getCapacity() != null) {
            asset.setCapacity(request.getCapacity());
        }
        if (request.getNote() != null) {
            asset.setNote(request.getNote());
        }

        asset = assetRepository.save(asset);
        return toResponse(asset);
    }

    @Override
    @Transactional
    public void deleteAsset(Long id) {
        AssetEntity asset = assetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phương tiện không tồn tại"));
        if (asset.getStatus() == AssetStatus.IN_USE) {
            throw new BusinessException("Không thể vô hiệu hóa phương tiện đang được sử dụng. Hãy bỏ gán đội trước.");
        }
        asset.setStatus(AssetStatus.INACTIVE);
        asset.setAssignedTeam(null);
        assetRepository.save(asset);
    }

    @Override
    @Transactional
    public AssetResponse changeStatus(Long id, AssetStatus newStatus) {
        AssetEntity asset = assetRepository.findByIdWithTeam(id)
                .orElseThrow(() -> new NotFoundException("Phương tiện không tồn tại"));

        AssetStatus currentStatus = asset.getStatus();
        if (currentStatus == newStatus) {
            throw new BusinessException("Phương tiện đã ở trạng thái " + newStatus);
        }

        Set<AssetStatus> allowed = VALID_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new BusinessException(
                    "Không thể chuyển từ " + currentStatus + " sang " + newStatus);
        }

        // Auto-unassign team when moving away from IN_USE
        if (currentStatus == AssetStatus.IN_USE && newStatus != AssetStatus.IN_USE) {
            asset.setAssignedTeam(null);
        }

        asset.setStatus(newStatus);
        asset = assetRepository.save(asset);
        return toResponse(asset);
    }

    @Override
    @Transactional
    public AssetResponse assignTeam(Long id, Long teamId) {
        AssetEntity asset = assetRepository.findByIdWithTeam(id)
                .orElseThrow(() -> new NotFoundException("Phương tiện không tồn tại"));

        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new BusinessException(
                    "Chỉ có thể gán phương tiện đang ở trạng thái AVAILABLE. Hiện tại: " + asset.getStatus());
        }

        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Đội cứu hộ không tồn tại"));
        if (team.getStatus() == null || team.getStatus() != 1) {
            throw new BusinessException("Không thể gán phương tiện cho đội không hoạt động");
        }

        asset.setAssignedTeam(team);
        asset.setStatus(AssetStatus.IN_USE);
        asset = assetRepository.save(asset);
        return toResponse(asset);
    }

    @Override
    @Transactional
    public AssetResponse unassignTeam(Long id) {
        AssetEntity asset = assetRepository.findByIdWithTeam(id)
                .orElseThrow(() -> new NotFoundException("Phương tiện không tồn tại"));

        if (asset.getAssignedTeam() == null) {
            throw new BusinessException("Phương tiện chưa được gán cho đội nào");
        }

        asset.setAssignedTeam(null);
        asset.setStatus(AssetStatus.AVAILABLE);
        asset = assetRepository.save(asset);
        return toResponse(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponse> getAssets(AssetStatus status) {
        return assetRepository.findAllWithTeam(status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponse> getAssetsFiltered(AssetStatus status, AssetType assetType, Long teamId) {
        return assetRepository.findAllWithFilters(status, assetType, teamId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponse> getAssetsByTeam(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new NotFoundException("Đội cứu hộ không tồn tại");
        }
        return assetRepository.findByAssignedTeamId(teamId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
