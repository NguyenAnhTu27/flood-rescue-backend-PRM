package com.floodrescue.module.relief.service;

import com.floodrescue.module.asset.entity.AssetEntity;
import com.floodrescue.module.asset.repository.AssetRepository;
import com.floodrescue.module.inventory.entity.InventoryIssueEntity;
import com.floodrescue.module.inventory.entity.ItemCategoryEntity;
import com.floodrescue.module.inventory.repository.IssueRepository;
import com.floodrescue.module.inventory.repository.ItemCategoryRepository;
import com.floodrescue.module.relief.dto.request.DistributionTaskAssignRequest;
import com.floodrescue.module.relief.dto.request.DistributionVoucherCreateRequest;
import com.floodrescue.module.relief.dto.response.DistributionVoucherResponse;
import com.floodrescue.module.relief.entity.DistributionAssignmentEntity;
import com.floodrescue.module.relief.entity.DistributionEntity;
import com.floodrescue.module.relief.entity.DistributionLineEntity;
import com.floodrescue.module.relief.entity.ReliefRequestEntity;
import com.floodrescue.module.relief.repository.DistributionAssignmentRepository;
import com.floodrescue.module.relief.repository.DistributionLineRepository;
import com.floodrescue.module.relief.repository.DistributionRepository;
import com.floodrescue.module.relief.repository.ReliefRequestRepository;
import com.floodrescue.module.team.entity.TeamEntity;
import com.floodrescue.module.team.repository.TeamRepository;
import com.floodrescue.shared.enums.DistributionPriority;
import com.floodrescue.shared.enums.InventoryDocumentStatus;
import com.floodrescue.shared.exception.BusinessException;
import com.floodrescue.shared.exception.NotFoundException;
import com.floodrescue.shared.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DistributionService {

    private final DistributionRepository distributionRepository;
    private final DistributionLineRepository distributionLineRepository;
    private final DistributionAssignmentRepository distributionAssignmentRepository;
    private final ReliefRequestRepository reliefRequestRepository;
    private final IssueRepository issueRepository;
    private final TeamRepository teamRepository;
    private final AssetRepository assetRepository;
    private final ItemCategoryRepository itemCategoryRepository;

    @Transactional
    public DistributionVoucherResponse createVoucher(Long userId, DistributionVoucherCreateRequest request) {
        if (userId == null) {
            throw new BusinessException("Không xác định được người tạo phiếu điều phối");
        }
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new BusinessException("Phiếu điều phối phải có ít nhất 1 dòng");
        }

        String code = StringUtils.hasText(request.getCode())
                ? request.getCode().trim()
                : CodeGenerator.generateDistributionVoucherCode();

        distributionRepository.findByCode(code).ifPresent(existing -> {
            throw new BusinessException("Mã phiếu điều phối đã tồn tại: " + code);
        });

        ReliefRequestEntity reliefRequest = null;
        if (request.getReliefRequestId() != null) {
            reliefRequest = reliefRequestRepository.findById(request.getReliefRequestId())
                    .orElseThrow(() -> new NotFoundException("Yêu cầu cứu trợ không tồn tại: " + request.getReliefRequestId()));
        }

        InventoryIssueEntity issue = resolveIssue(request.getIssueId(), request.getIssueRefCode());
        TeamEntity team = resolveTeam(request.getTeamId());
        AssetEntity asset = resolveAsset(request.getAssetId());

        DistributionEntity distribution = DistributionEntity.builder()
                .code(code)
                .status(InventoryDocumentStatus.DRAFT)
                .createdById(userId)
                .reliefRequest(reliefRequest)
                .issue(issue)
                .issueRefCode(issue != null ? issue.getCode() : trimToNull(request.getIssueRefCode()))
                .team(team)
                .asset(asset)
                .receiverName(trimToNull(request.getReceiverName()))
                .receiverPhone(trimToNull(request.getReceiverPhone()))
                .deliveryAddress(trimToNull(request.getDeliveryAddress()))
                .eta(request.getEta())
                .priority(request.getPriority() != null ? request.getPriority() : DistributionPriority.TRUNG_BINH)
                .note(trimToNull(request.getNote()))
                .build();

        DistributionEntity saved = distributionRepository.save(distribution);

        List<DistributionLineEntity> lines = request.getLines().stream()
                .map(lineRequest -> {
                    ItemCategoryEntity category = itemCategoryRepository.findById(lineRequest.getItemCategoryId())
                            .orElseThrow(() -> new NotFoundException("Loại hàng không tồn tại: " + lineRequest.getItemCategoryId()));

                    String unit = StringUtils.hasText(lineRequest.getUnit()) ? lineRequest.getUnit().trim() : category.getUnit();

                    return DistributionLineEntity.builder()
                            .distribution(saved)
                            .itemCategory(category)
                            .qty(BigDecimal.valueOf(lineRequest.getQty()))
                            .unit(unit)
                            .build();
                }).collect(Collectors.toList());

        distributionLineRepository.saveAll(lines);
        return toResponse(saved, lines);
    }

    @Transactional(readOnly = true)
    public DistributionVoucherResponse getVoucher(Long id) {
        DistributionEntity distribution = distributionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phiếu điều phối không tồn tại"));
        List<DistributionLineEntity> lines = distributionLineRepository.findByDistribution(distribution);
        return toResponse(distribution, lines);
    }

    @Transactional(readOnly = true)
    public Page<DistributionVoucherResponse> listVouchers(InventoryDocumentStatus status, Pageable pageable) {
        Page<DistributionEntity> page = status != null
                ? distributionRepository.findByStatus(status, pageable)
                : distributionRepository.findAll(pageable);
        return page.map(distribution -> toResponse(distribution, distributionLineRepository.findByDistribution(distribution)));
    }

    @Transactional
    public DistributionVoucherResponse assignTask(Long id, Long userId, DistributionTaskAssignRequest request) {
        if (userId == null) {
            throw new BusinessException("Không xác định được người gán nhiệm vụ");
        }
        if (request == null) {
            throw new BusinessException("Payload gán nhiệm vụ không được để trống");
        }

        DistributionEntity distribution = distributionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phiếu điều phối không tồn tại: " + id));

        if (request.getTeamId() == null) {
            throw new BusinessException("teamId không được để trống khi gán nhiệm vụ");
        }

        TeamEntity team = resolveTeam(request.getTeamId());
        AssetEntity asset = resolveAsset(request.getAssetId());

        distribution.setTeam(team);
        distribution.setAsset(asset);

        if (request.getEta() != null) {
            distribution.setEta(request.getEta());
        }

        if (StringUtils.hasText(request.getNote())) {
            distribution.setNote(request.getNote().trim());
        }

        InventoryDocumentStatus nextStatus = parseStatus(request.getStatus());
        distribution.setStatus(nextStatus);

        DistributionEntity saved = distributionRepository.save(distribution);

        DistributionAssignmentEntity assignment = DistributionAssignmentEntity.builder()
                .distribution(saved)
                .team(team)
                .asset(asset)
                .receiverName(saved.getReceiverName())
                .receiverPhone(saved.getReceiverPhone())
                .deliveryAddress(saved.getDeliveryAddress())
                .eta(saved.getEta())
                .status(nextStatus)
                .note(saved.getNote())
                .build();
        distributionAssignmentRepository.save(assignment);

        List<DistributionLineEntity> lines = distributionLineRepository.findByDistribution(saved);
        return toResponse(saved, lines);
    }

    private InventoryIssueEntity resolveIssue(Long issueId, String issueRefCode) {
        if (issueId != null) {
            return issueRepository.findById(issueId)
                    .orElseThrow(() -> new NotFoundException("Phiếu xuất kho không tồn tại: " + issueId));
        }
        if (StringUtils.hasText(issueRefCode)) {
            return issueRepository.findByCode(issueRefCode.trim())
                    .orElseThrow(() -> new NotFoundException("Phiếu xuất kho không tồn tại với mã: " + issueRefCode));
        }
        return null;
    }

    private TeamEntity resolveTeam(Long teamId) {
        if (teamId == null) {
            return null;
        }
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Đội giao hàng không tồn tại: " + teamId));
    }

    private AssetEntity resolveAsset(Long assetId) {
        if (assetId == null) {
            return null;
        }
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("Phương tiện không tồn tại: " + assetId));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private InventoryDocumentStatus parseStatus(String rawStatus) {
        if (!StringUtils.hasText(rawStatus)) {
            return InventoryDocumentStatus.ASSIGNED;
        }
        try {
            return InventoryDocumentStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Trạng thái điều phối không hợp lệ: " + rawStatus);
        }
    }

    private DistributionVoucherResponse toResponse(DistributionEntity distribution, List<DistributionLineEntity> lines) {
        DistributionVoucherResponse.DistributionVoucherResponseBuilder builder = DistributionVoucherResponse.builder()
                .id(distribution.getId())
                .code(distribution.getCode())
                .status(distribution.getStatus())
                .createdById(distribution.getCreatedById())
                .issueId(distribution.getIssue() != null ? distribution.getIssue().getId() : null)
                .issueCode(distribution.getIssue() != null ? distribution.getIssue().getCode() : distribution.getIssueRefCode())
                .receiverName(distribution.getReceiverName())
                .receiverPhone(distribution.getReceiverPhone())
                .deliveryAddress(distribution.getDeliveryAddress())
                .eta(distribution.getEta())
                .priority(distribution.getPriority())
                .note(distribution.getNote())
                .createdAt(distribution.getCreatedAt())
                .updatedAt(distribution.getUpdatedAt())
                .lines(lines.stream().map(line -> DistributionVoucherResponse.LineItem.builder()
                        .id(line.getId())
                        .itemCategoryId(line.getItemCategory().getId())
                        .itemCode(line.getItemCategory().getCode())
                        .itemName(line.getItemCategory().getName())
                        .qty(line.getQty())
                        .unit(line.getUnit())
                        .build()).collect(Collectors.toList()));

        if (distribution.getReliefRequest() != null) {
            builder.reliefRequestId(distribution.getReliefRequest().getId())
                    .reliefRequestCode(distribution.getReliefRequest().getCode());
        }

        if (distribution.getTeam() != null) {
            builder.teamId(distribution.getTeam().getId())
                    .teamCode(distribution.getTeam().getCode())
                    .teamName(distribution.getTeam().getName());
        }

        if (distribution.getAsset() != null) {
            builder.assetId(distribution.getAsset().getId())
                    .assetCode(distribution.getAsset().getCode())
                    .assetName(distribution.getAsset().getName());
        }

        return builder.build();
    }
}
