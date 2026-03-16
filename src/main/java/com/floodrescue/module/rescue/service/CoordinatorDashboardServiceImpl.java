package com.floodrescue.module.rescue.service;

import com.floodrescue.module.asset.repository.AssetRepository;
import com.floodrescue.module.rescue.dto.response.CoordinatorDashboardResponse;
import com.floodrescue.module.rescue.repository.RescueAssignmentRepository;
import com.floodrescue.shared.enums.AssetType;
import com.floodrescue.module.rescue.repository.RescueRequestRepository;
import com.floodrescue.module.rescue.repository.TaskGroupRepository;
import com.floodrescue.module.team.repository.TeamRepository;
import com.floodrescue.shared.enums.RescueRequestStatus;
import com.floodrescue.shared.enums.TaskGroupStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoordinatorDashboardServiceImpl implements CoordinatorDashboardService {

    private final RescueRequestRepository rescueRequestRepository;
    private final TeamRepository teamRepository;
    private final AssetRepository assetRepository;
    private final RescueAssignmentRepository rescueAssignmentRepository;
    private final TaskGroupRepository taskGroupRepository;

    @Override
    @Transactional(readOnly = true)
    public CoordinatorDashboardResponse getDashboard(Long coordinatorId) {
        // 1) Queue: lấy PENDING theo priority (top 50)
        var pendingPage = rescueRequestRepository.findPendingRequestsOrderedByPriority(
                RescueRequestStatus.PENDING,
                PageRequest.of(0, 50)
        );

        var queue = pendingPage.getContent().stream()
                .map(r -> CoordinatorDashboardResponse.QueueItem.builder()
                        .id(r.getId())
                        .code(r.getCode())
                        .priority(r.getPriority() != null ? r.getPriority().name() : null)
                        .peopleCount(r.getAffectedPeopleCount())
                        .status(r.getStatus() != null ? r.getStatus().name() : null)
                        .waitingForTeam(r.getWaitingForTeam())
                        .timeAgo(formatTimeAgo(r.getCreatedAt()))
                        .lat(r.getLatitude())
                        .lng(r.getLongitude())
                        .build())
                .toList();

        // 2) Teams: BUSY khi có ít nhất 1 task group IN_PROGRESS
        Set<Long> busyTeamIds = taskGroupRepository.findByStatus(
                        TaskGroupStatus.IN_PROGRESS,
                        PageRequest.of(0, 1000)
                ).getContent().stream()
                .filter(g -> g.getAssignedTeam() != null && g.getAssignedTeam().getId() != null)
                .map(g -> g.getAssignedTeam().getId())
                .collect(Collectors.toSet());

        var teams = teamRepository.findAll().stream()
                .map(t -> CoordinatorDashboardResponse.TeamItem.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .area(t.getCurrentLocationText() != null ? t.getCurrentLocationText() : t.getDescription())
                        .status(busyTeamIds.contains(t.getId()) ? "BUSY" : "AVAILABLE")
                        .distance(null)
                        .lastUpdate(formatTimeAgo(t.getCurrentLocationUpdatedAt()))
                        .lat(t.getCurrentLatitude())
                        .lng(t.getCurrentLongitude())
                        .online(true)
                        .build())
                .toList();

        // 3) Vehicles (assets)
        var vehicles = assetRepository.findAll().stream()
                .map(a -> CoordinatorDashboardResponse.VehicleItem.builder()
                        .id(a.getId())
                        .code(a.getCode())
                        .name(a.getName())
                        .type(mapAssetType(a.getAssetType()))
                        .capacity(a.getCapacity())
                        .status(a.getStatus() != null ? a.getStatus().name() : null)
                        .distance(null)
                        .location(a.getNote())
                        .online(a.getStatus() == null || !"INACTIVE".equals(a.getStatus().name()))
                        .build())
                .toList();

        return CoordinatorDashboardResponse.builder()
                .requests(queue)
                .teams(teams)
                .vehicles(vehicles)
                .build();
    }

    private String formatTimeAgo(LocalDateTime createdAt) {
        if (createdAt == null) return null;
        Duration d = Duration.between(createdAt, LocalDateTime.now());
        long minutes = Math.max(0, d.toMinutes());
        if (minutes < 60) return minutes + "p trước";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h trước";
        long days = hours / 24;
        return days + "d trước";
    }

    private String mapAssetType(AssetType assetType) {
        if (assetType == null) return "boat";
        return switch (assetType) {
            case CANO -> "cano";
            case HELICOPTER -> "helicopter";
            case BOAT -> "boat";
            default -> "boat";
        };
    }
}
