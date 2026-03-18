package com.floodrescue.module.relief.service;

import com.floodrescue.module.asset.repository.AssetReponsitory;
import com.floodrescue.module.relief.dto.response.ManagerReliefDispatchDashboardResponse;
import com.floodrescue.module.relief.reponsitory.ReliefRequestRepository;
import com.floodrescue.module.team.repository.TeamRepository;
import com.floodrescue.shared.enums.AssetStatus;
import com.floodrescue.shared.enums.InventoryDocumentStatus;
import com.floodrescue.shared.enums.ReliefDeliveryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerReliefDispatchDashboardService {

    private final ReliefRequestRepository reliefRequestRepository;
    private final TeamRepository teamRepository;
    private final AssetReponsitory assetRepository;

    @Transactional(readOnly = true)
    public ManagerReliefDispatchDashboardResponse getDashboard() {
        var reliefRequests = reliefRequestRepository.findAll(
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        var queue = reliefRequests.stream()
                .map(r -> ManagerReliefDispatchDashboardResponse.QueueItem.builder()
                        .id(r.getId())
                        .code(r.getCode())
                        .priority("MEDIUM")
                        .peopleCount(extractPeopleCount(r.getNote()))
                        .timeAgo(formatTimeAgo(r.getCreatedAt()))
                        .status(r.getStatus() != null ? r.getStatus().name() : "DRAFT")
                        .waitingForTeam(
                                r.getStatus() == InventoryDocumentStatus.APPROVED
                                        && r.getAssignedTeamId() == null
                        )
                        .lat(r.getLatitude())
                        .lng(r.getLongitude())
                        .build())
                .toList();

        Set<Long> busyTeamIds = reliefRequests.stream()
                .filter(r -> r.getAssignedTeamId() != null)
                .filter(r -> r.getStatus() == InventoryDocumentStatus.APPROVED || r.getStatus() == InventoryDocumentStatus.DRAFT)
                .filter(r -> r.getDeliveryStatus() != ReliefDeliveryStatus.COMPLETED)
                .map(r -> r.getAssignedTeamId())
                .collect(Collectors.toSet());

        var teams = teamRepository.findAll().stream()
                .map(t -> ManagerReliefDispatchDashboardResponse.TeamItem.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .area(t.getDescription())
                        .status(busyTeamIds.contains(t.getId()) ? "BUSY" : "AVAILABLE")
                        .distance(null)
                        .lastUpdate(t.getCurrentLocationUpdatedAt() == null ? null : formatTimeAgo(t.getCurrentLocationUpdatedAt()))
                        .lat(t.getCurrentLatitude())
                        .lng(t.getCurrentLongitude())
                        .online(Byte.valueOf((byte) 1).equals(t.getStatus()))
                        .build())
                .toList();

        var vehicles = assetRepository.findAll().stream()
                .map(a -> ManagerReliefDispatchDashboardResponse.VehicleItem.builder()
                        .id(a.getId())
                        .code(a.getCode())
                        .name(a.getName())
                        .type(mapAssetType(a.getAssetType()))
                        .capacity(a.getCapacity())
                        .status(a.getStatus() != null ? a.getStatus().name() : "AVAILABLE")
                        .distance(null)
                        .location(a.getNote())
                        .online(a.getStatus() != AssetStatus.INACTIVE)
                        .build())
                .toList();

        return ManagerReliefDispatchDashboardResponse.builder()
                .requests(queue)
                .teams(teams)
                .vehicles(vehicles)
                .build();
    }

    private Integer extractPeopleCount(String note) {
        if (note == null || note.isBlank()) return 1;
        String marker = "Số người cần hỗ trợ:";
        int idx = note.indexOf(marker);
        if (idx < 0) return 1;
        String sub = note.substring(idx + marker.length()).trim();
        StringBuilder digits = new StringBuilder();
        for (char c : sub.toCharArray()) {
            if (Character.isDigit(c)) digits.append(c);
            else if (digits.length() > 0) break;
        }
        if (digits.isEmpty()) return 1;
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            return 1;
        }
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

    private String mapAssetType(String assetType) {
        if (assetType == null) return "boat";
        String t = assetType.trim().toUpperCase();
        return switch (t) {
            case "CANO" -> "cano";
            case "HELICOPTER" -> "helicopter";
            case "BOAT" -> "boat";
            default -> "boat";
        };
    }
}
