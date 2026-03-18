package com.floodrescue.module.rescue.service;

import com.floodrescue.module.asset.entity.AssetEntity;
import com.floodrescue.module.asset.repository.AssetReponsitory;
import com.floodrescue.module.rescue.dto.response.RescuerDashboardResponse;
import com.floodrescue.module.rescue.dto.response.TaskGroupResponse;
import com.floodrescue.module.rescue.dto.request.EscalateTaskGroupRequest;
import com.floodrescue.module.rescue.entity.RescueAssigmentEntity;
import com.floodrescue.module.rescue.entity.RescueRequestEntity;
import com.floodrescue.module.rescue.entity.TaskGroupEntity;
import com.floodrescue.module.rescue.entity.TaskGroupRequestEntity;
import com.floodrescue.module.rescue.entity.TaskGroupTimelineEntity;
import com.floodrescue.module.rescue.mapper.TaskGroupMapper;
import com.floodrescue.module.rescue.repository.RescueAssignmentRepository;
import com.floodrescue.module.rescue.repository.RescueRequestRepository;
import com.floodrescue.module.rescue.repository.TaskGroupRepository;
import com.floodrescue.module.rescue.repository.TaskGroupRequestRepository;
import com.floodrescue.module.rescue.repository.TaskGroupTimelineRepository;
import com.floodrescue.module.notification.dto.EmergencyAckResponse;
import com.floodrescue.module.notification.service.NotificationService;
import com.floodrescue.module.team.entity.TeamEntity;
import com.floodrescue.module.team.repository.TeamRepository;
import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.module.user.repository.UserRepository;
import com.floodrescue.shared.enums.AssetStatus;
import com.floodrescue.shared.enums.RescueRequestStatus;
import com.floodrescue.shared.enums.TaskGroupStatus;
import com.floodrescue.shared.enums.RescuePriority;
import com.floodrescue.shared.exception.BusinessException;
import com.floodrescue.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RescuerTaskServiceImpl implements RescuerTaskService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TaskGroupRepository taskGroupRepository;
    private final TaskGroupRequestRepository taskGroupRequestRepository;
    private final TaskGroupTimelineRepository taskGroupTimelineRepository;
    private final RescueAssignmentRepository rescueAssignmentRepository;
    private final RescueRequestRepository rescueRequestRepository;
    private final AssetReponsitory assetRepository;
    private final TaskGroupMapper taskGroupMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public RescuerDashboardResponse getDashboard(Long rescuerUserId) {
        Long teamId = getRequiredTeamId(rescuerUserId);

        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Đội cứu hộ không tồn tại"));

        // Active assignments of this team
        List<RescueAssigmentEntity> activeAssignments = rescueAssignmentRepository.findByTeamIdAndIsActiveTrue(teamId);

        // Only active task groups should appear on rescuer dashboard.
        List<TaskGroupStatus> activeStatuses = List.of(
                TaskGroupStatus.NEW,
                TaskGroupStatus.ASSIGNED,
                TaskGroupStatus.IN_PROGRESS
        );
        Page<TaskGroupEntity> groupsPage = taskGroupRepository.findByAssignedTeamIdAndStatusIn(
                teamId,
                activeStatuses,
                Pageable.ofSize(10)
        );
        List<TaskGroupResponse> groups = groupsPage.getContent().stream()
                .map(taskGroupMapper::toResponse)
                .toList();
        List<RescuerDashboardResponse.HeldAssetItem> heldAssets = assetRepository.findByAssignedTeamId(teamId).stream()
                .map(asset -> RescuerDashboardResponse.HeldAssetItem.builder()
                        .id(asset.getId())
                        .code(asset.getCode())
                        .name(asset.getName())
                        .assetType(asset.getAssetType())
                        .status(asset.getStatus() != null ? asset.getStatus().name() : null)
                        .build())
                .toList();

        return RescuerDashboardResponse.builder()
                .teamId(team.getId())
                .teamName(team.getName())
                .teamLatitude(team.getCurrentLatitude())
                .teamLongitude(team.getCurrentLongitude())
                .teamLocationText(team.getCurrentLocationText())
                .teamLocationUpdatedAt(team.getCurrentLocationUpdatedAt())
                .activeAssignments((long) activeAssignments.size())
                .activeTaskGroups(groupsPage.getTotalElements())
                .heldAssets(heldAssets)
                .taskGroups(groups)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskGroupResponse> getMyTaskGroups(Long rescuerUserId, TaskGroupStatus status, Pageable pageable) {
        Long teamId = getRequiredTeamId(rescuerUserId);

        Page<TaskGroupEntity> page;
        if (status != null) {
            page = taskGroupRepository.findByAssignedTeamIdAndStatus(teamId, status, pageable);
        } else {
            page = taskGroupRepository.findByAssignedTeamId(teamId, pageable);
        }
        return page.map(taskGroupMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskGroupResponse getMyTaskGroup(Long rescuerUserId, Long taskGroupId) {
        Long teamId = getRequiredTeamId(rescuerUserId);

        TaskGroupEntity group = taskGroupRepository.findById(taskGroupId)
                .orElseThrow(() -> new NotFoundException("Nhóm nhiệm vụ không tồn tại"));

        verifyGroupBelongsToTeam(group, teamId);

        List<TaskGroupRequestEntity> links = taskGroupRequestRepository.findByTaskGroupId(group.getId());
        List<RescueAssigmentEntity> assignments = rescueAssignmentRepository.findByTaskGroupIdAndIsActiveTrue(group.getId());
        List<TaskGroupTimelineEntity> timeline = taskGroupTimelineRepository.findByTaskGroupIdOrderByCreatedAtDesc(group.getId());

        return taskGroupMapper.toResponseWithDetails(group, links, assignments, timeline);
    }

    @Override
    @Transactional
    public TaskGroupResponse updateMyTaskGroupStatus(Long rescuerUserId, Long taskGroupId, TaskGroupStatus status, String note) {
        if (status == null) {
            throw new BusinessException("Trạng thái không được để trống");
        }

        Long teamId = getRequiredTeamId(rescuerUserId);

        TaskGroupEntity group = taskGroupRepository.findById(taskGroupId)
                .orElseThrow(() -> new NotFoundException("Nhóm nhiệm vụ không tồn tại"));

        verifyGroupBelongsToTeam(group, teamId);

        TaskGroupStatus oldStatus = group.getStatus();
        group.setStatus(status);
        taskGroupRepository.save(group);
        syncLinkedRescueRequestStatus(group.getId(), status, note);

        // Timeline (optional) - reuse existing timeline table
        UserEntity actor = userRepository.findById(rescuerUserId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        TaskGroupTimelineEntity tl = TaskGroupTimelineEntity.builder()
                .taskGroup(group)
                .actor(actor)
                .eventType("RESCUER_STATUS_CHANGE")
                .note(buildStatusChangeNote(oldStatus, status, note))
                .build();
        taskGroupTimelineRepository.save(tl);

        if (status == TaskGroupStatus.CANCELLED) {
            String cancelReason = (note == null || note.isBlank()) ? "Không có mô tả từ đội cứu hộ" : note.trim();
            List<TaskGroupRequestEntity> links = taskGroupRequestRepository.findByTaskGroupId(group.getId());
            List<Long> citizenIds = links.stream()
                    .map(TaskGroupRequestEntity::getRescueRequest)
                    .filter(java.util.Objects::nonNull)
                    .map(RescueRequestEntity::getCitizen)
                    .filter(java.util.Objects::nonNull)
                    .map(UserEntity::getId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();

            notificationService.notifyRole(
                    "COORDINATOR",
                    "Đội cứu hộ đã hủy nhiệm vụ",
                    "Đội " + (group.getAssignedTeam() != null ? group.getAssignedTeam().getName() : "không rõ")
                            + " vừa chuyển nhiệm vụ " + (group.getCode() != null ? group.getCode() : ("#" + group.getId()))
                            + " sang CANCELLED. Lý do: " + cancelReason,
                    "RESCUER_CANCELLED_TASK_GROUP",
                    "TASK_GROUP",
                    group.getId(),
                    true,
                    group.getAssignedTeam() != null ? group.getAssignedTeam().getId() : null
            );

            if (!citizenIds.isEmpty()) {
                notificationService.notifyUsers(
                        citizenIds,
                        "Yêu cầu cứu hộ tạm dừng",
                        "Đội cứu hộ đã tạm dừng yêu cầu của bạn do: " + cancelReason
                                + ". Bạn có thể đánh giá dịch vụ hoặc gửi lại yêu cầu.",
                        "RESCUER_CANCELLED_REQUEST",
                        "TASK_GROUP",
                        group.getId(),
                        true,
                        group.getAssignedTeam() != null ? group.getAssignedTeam().getId() : null
                );
            }
        }

        if (status == TaskGroupStatus.DONE) {
            List<TaskGroupRequestEntity> links = taskGroupRequestRepository.findByTaskGroupId(group.getId());
            List<Long> citizenIds = links.stream()
                    .map(TaskGroupRequestEntity::getRescueRequest)
                    .filter(java.util.Objects::nonNull)
                    .map(RescueRequestEntity::getCitizen)
                    .filter(java.util.Objects::nonNull)
                    .map(UserEntity::getId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();

            if (!citizenIds.isEmpty()) {
                notificationService.notifyUsers(
                        citizenIds,
                        "Đội cứu hộ vừa hoàn thành yêu cầu",
                        "Đội cứu hộ vừa hoàn thành yêu cầu của bạn. Vui lòng xác nhận: bạn đã được cứu hộ chưa?",
                        "RESCUER_COMPLETED_REQUEST",
                        "TASK_GROUP",
                        group.getId(),
                        true,
                        group.getAssignedTeam() != null ? group.getAssignedTeam().getId() : null
                );
            }
        }

        return getMyTaskGroup(rescuerUserId, taskGroupId);
    }

    @Override
    @Transactional
    public long returnMyTeamAssets(Long rescuerUserId) {
        Long teamId = getRequiredTeamId(rescuerUserId);
        List<TaskGroupStatus> activeStatuses = List.of(TaskGroupStatus.NEW, TaskGroupStatus.ASSIGNED, TaskGroupStatus.IN_PROGRESS);
        boolean hasActiveTask = taskGroupRepository
                .findByAssignedTeamIdAndStatusIn(teamId, activeStatuses, Pageable.ofSize(1))
                .hasContent();
        if (hasActiveTask) {
            throw new BusinessException("Không thể trả tài sản khi đội vẫn còn nhiệm vụ đang hoạt động");
        }

        List<AssetEntity> heldAssets = assetRepository.findByAssignedTeamId(teamId);
        if (heldAssets.isEmpty()) {
            return 0L;
        }

        List<RescueAssigmentEntity> activeAssignments = rescueAssignmentRepository.findByTeamIdAndIsActiveTrue(teamId);
        if (!activeAssignments.isEmpty()) {
            activeAssignments.forEach(a -> a.setIsActive(false));
            rescueAssignmentRepository.saveAll(activeAssignments);
        }

        heldAssets.forEach(asset -> {
            asset.setStatus(AssetStatus.AVAILABLE);
            asset.setAssignedTeam(null);
        });
        assetRepository.saveAll(heldAssets);

        return heldAssets.size();
    }

    @Override
    @Transactional
    public TaskGroupResponse escalateMyTaskGroup(Long rescuerUserId, Long taskGroupId, EscalateTaskGroupRequest request) {
        Long teamId = getRequiredTeamId(rescuerUserId);

        TaskGroupEntity group = taskGroupRepository.findById(taskGroupId)
                .orElseThrow(() -> new NotFoundException("Nhóm nhiệm vụ không tồn tại"));
        verifyGroupBelongsToTeam(group, teamId);

        List<TaskGroupRequestEntity> links = taskGroupRequestRepository.findByTaskGroupId(group.getId());
        for (TaskGroupRequestEntity link : links) {
            Long requestId = link.getRescueRequest().getId();
            RescueRequestEntity rr = rescueRequestRepository.findById(requestId)
                    .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại: " + requestId));
            rr.setPriority(RescuePriority.HIGH);
            rescueRequestRepository.save(rr);
        }

        UserEntity actor = userRepository.findById(rescuerUserId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        String severity = String.valueOf(request.getSeverity()).toUpperCase();
        String reason = request.getReason();

        TaskGroupTimelineEntity tl = TaskGroupTimelineEntity.builder()
                .taskGroup(group)
                .actor(actor)
                .eventType("RESCUER_EMERGENCY")
                .note("[KHAN_CAP/" + severity + "] " + reason)
                .build();
        taskGroupTimelineRepository.save(tl);

        notificationService.notifyRole(
                "COORDINATOR",
                "Khẩn cấp từ đội cứu hộ",
                "Đội " + (group.getAssignedTeam() != null ? group.getAssignedTeam().getName() : "không rõ")
                        + " gửi khẩn cấp cho nhiệm vụ " + group.getCode()
                        + ". Lý do: " + reason,
                "RESCUER_EMERGENCY",
                "TASK_GROUP",
                group.getId(),
                true,
                group.getAssignedTeam() != null ? group.getAssignedTeam().getId() : null
        );

        return getMyTaskGroup(rescuerUserId, taskGroupId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyAckResponse> getEmergencyAcks(Long rescuerUserId, Long taskGroupId) {
        Long teamId = getRequiredTeamId(rescuerUserId);
        TaskGroupEntity group = taskGroupRepository.findById(taskGroupId)
                .orElseThrow(() -> new NotFoundException("Nhóm nhiệm vụ không tồn tại"));
        verifyGroupBelongsToTeam(group, teamId);
        return notificationService.getCoordinatorEmergencyAcks(taskGroupId);
    }

    @Override
    @Transactional
    public void updateMyTeamLocation(Long rescuerUserId, Double latitude, Double longitude, String locationText) {
        Long teamId = getRequiredTeamId(rescuerUserId);
        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Đội cứu hộ không tồn tại"));
        team.setCurrentLatitude(latitude);
        team.setCurrentLongitude(longitude);
        team.setCurrentLocationText(locationText);
        team.setCurrentLocationUpdatedAt(java.time.LocalDateTime.now());
        teamRepository.save(team);
    }

    private String buildStatusChangeNote(TaskGroupStatus from, TaskGroupStatus to, String note) {
        String base = "Status changed: " + (from != null ? from.name() : "UNKNOWN") + " -> " + to.name();
        if (note == null || note.isBlank()) {
            return base;
        }
        return base + " | " + note.trim();
    }

    private void syncLinkedRescueRequestStatus(Long taskGroupId, TaskGroupStatus taskStatus, String note) {
        RescueRequestStatus mappedStatus = mapTaskGroupStatusToRescueRequestStatus(taskStatus);
        if (mappedStatus == null) {
            return;
        }
        List<TaskGroupRequestEntity> links = taskGroupRequestRepository.findByTaskGroupId(taskGroupId);
        if (links.isEmpty()) {
            return;
        }
        List<RescueRequestEntity> requests = links.stream()
                .map(TaskGroupRequestEntity::getRescueRequest)
                .toList();
        for (RescueRequestEntity rr : requests) {
            rr.setStatus(mappedStatus);
            if (mappedStatus == RescueRequestStatus.COMPLETED) {
                rr.setRescueResultConfirmationStatus("PENDING");
                rr.setRescueResultConfirmationNote(null);
                rr.setRescueResultConfirmedAt(null);
            }
            if (mappedStatus == RescueRequestStatus.CANCELLED) {
                rr.setCoordinatorCancelNote((note == null || note.isBlank()) ? "Đội cứu hộ hủy yêu cầu." : note.trim());
            }
        }
        rescueRequestRepository.saveAll(requests);
    }

    private RescueRequestStatus mapTaskGroupStatusToRescueRequestStatus(TaskGroupStatus taskStatus) {
        return switch (taskStatus) {
            case ASSIGNED -> RescueRequestStatus.ASSIGNED;
            case IN_PROGRESS -> RescueRequestStatus.IN_PROGRESS;
            case DONE -> RescueRequestStatus.COMPLETED;
            case CANCELLED -> RescueRequestStatus.CANCELLED;
            default -> null;
        };
    }

    private Long getRequiredTeamId(Long rescuerUserId) {
        UserEntity user = userRepository.findById(rescuerUserId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        if (user.getTeamId() == null) {
            throw new BusinessException("Tài khoản đội cứu hộ chưa được gán vào đội");
        }
        return user.getTeamId();
    }

    private void verifyGroupBelongsToTeam(TaskGroupEntity group, Long teamId) {
        if (group.getAssignedTeam() == null || group.getAssignedTeam().getId() == null) {
            throw new BusinessException("Nhóm nhiệm vụ chưa được phân công cho đội nào");
        }
        if (!group.getAssignedTeam().getId().equals(teamId)) {
            throw new BusinessException("Bạn không có quyền truy cập nhóm nhiệm vụ của đội khác");
        }
    }
}
