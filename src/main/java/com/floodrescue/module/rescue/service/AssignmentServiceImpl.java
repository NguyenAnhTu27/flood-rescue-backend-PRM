package com.floodrescue.module.rescue.service;

import com.floodrescue.module.asset.entity.AssetEntity;
import com.floodrescue.module.asset.repository.AssetRepository;
import com.floodrescue.module.notification.service.NotificationService;
import com.floodrescue.module.rescue.dto.request.AssignTaskGroupRequest;
import com.floodrescue.module.rescue.dto.response.AssignmentResponse;
import com.floodrescue.module.rescue.dto.response.TaskGroupResponse;
import com.floodrescue.module.rescue.entity.RescueAssignmentEntity;
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
import com.floodrescue.module.team.entity.TeamEntity;
import com.floodrescue.module.team.repository.TeamRepository;
import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.module.user.repository.UserRepository;
import com.floodrescue.shared.enums.AssetStatus;
import com.floodrescue.shared.enums.RescueRequestStatus;
import com.floodrescue.shared.enums.TaskGroupStatus;
import com.floodrescue.shared.exception.BusinessException;
import com.floodrescue.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final TaskGroupRepository taskGroupRepository;
    private final TaskGroupRequestRepository taskGroupRequestRepository;
    private final TaskGroupTimelineRepository taskGroupTimelineRepository;
    private final RescueAssignmentRepository rescueAssignmentRepository;
    private final RescueRequestRepository rescueRequestRepository;
    private final TeamRepository teamRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final TaskGroupMapper mapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public TaskGroupResponse assignTaskGroup(AssignTaskGroupRequest request, Long coordinatorId) {
        TaskGroupEntity group = taskGroupRepository.findById(request.getTaskGroupId())
                .orElseThrow(() -> new NotFoundException("Nhóm nhiệm vụ không tồn tại"));

        TeamEntity team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new NotFoundException("Đội cứu hộ không tồn tại"));

        List<TaskGroupRequestEntity> links = taskGroupRequestRepository.findByTaskGroupId(group.getId());
        List<RescueRequestEntity> requestEntities = links.stream()
                .map(link -> rescueRequestRepository.findById(link.getRescueRequest().getId())
                        .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại: " + link.getRescueRequest().getId())))
                .toList();
        for (RescueRequestEntity rr : requestEntities) {
            if (Boolean.TRUE.equals(rr.getIsEmergency())
                    && rr.getSourceTeamId() != null
                    && rr.getSourceTeamId().equals(team.getId())) {
                throw new BusinessException("Không thể phân công lại đúng đội vừa gửi yêu cầu khẩn cấp");
            }
        }

        AssetEntity asset = null;
        if (request.getAssetId() != null) {
            asset = assetRepository.findById(request.getAssetId())
                    .orElseThrow(() -> new NotFoundException("Phương tiện / thiết bị không tồn tại"));

            if (asset.getAssignedTeam() != null
                    && asset.getAssignedTeam().getId() != null
                    && !asset.getAssignedTeam().getId().equals(team.getId())) {
                throw new BusinessException("Phương tiện đang thuộc đội khác, không thể phân cho đội này");
            }

            boolean isHeldBySameTeam = asset.getAssignedTeam() != null
                    && asset.getAssignedTeam().getId() != null
                    && asset.getAssignedTeam().getId().equals(team.getId());

            if (!isHeldBySameTeam && asset.getStatus() != AssetStatus.AVAILABLE) {
                throw new BusinessException("Phương tiện / thiết bị không sẵn sàng");
            }

            if (isHeldBySameTeam
                    && asset.getStatus() != AssetStatus.AVAILABLE
                    && asset.getStatus() != AssetStatus.IN_USE) {
                throw new BusinessException("Phương tiện của đội hiện không khả dụng cho nhiệm vụ");
            }
        }

        UserEntity coordinator = userRepository.findById(coordinatorId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));

        // Deactivate previous active assignments
        List<RescueAssignmentEntity> currentAssignments =
                rescueAssignmentRepository.findByTaskGroupIdAndIsActiveTrue(group.getId());
        currentAssignments.forEach(a -> a.setIsActive(false));
        if (!currentAssignments.isEmpty()) {
            rescueAssignmentRepository.saveAll(currentAssignments);
        }

        // Create new assignment
        RescueAssignmentEntity assignment = RescueAssignmentEntity.builder()
                .taskGroup(group)
                .team(team)
                .asset(asset)
                .assignedBy(coordinator)
                .isActive(true)
                .build();

        assignment = rescueAssignmentRepository.save(assignment);

        // Update asset status if present
        if (asset != null) {
            asset.setStatus(AssetStatus.IN_USE);
            asset.setAssignedTeam(team);
            assetRepository.save(asset);
        }

        // Update group status and assigned team
        group.setAssignedTeam(team);
        if (group.getStatus() == TaskGroupStatus.NEW) {
            group.setStatus(TaskGroupStatus.ASSIGNED);
        }
        taskGroupRepository.save(group);
        syncAssignedRequestStatuses(requestEntities);

        // Timeline
        TaskGroupTimelineEntity tl = TaskGroupTimelineEntity.builder()
                .taskGroup(group)
                .actor(coordinator)
                .eventType("ASSIGN")
                .note(request.getNote())
                .build();
        taskGroupTimelineRepository.save(tl);

        List<RescueAssignmentEntity> allAssignments =
                rescueAssignmentRepository.findByTaskGroupIdAndIsActiveTrue(group.getId());
        List<TaskGroupTimelineEntity> timeline =
                taskGroupTimelineRepository.findByTaskGroupIdOrderByCreatedAtDesc(group.getId());

        for (RescueRequestEntity rr : requestEntities) {
            if (Boolean.TRUE.equals(rr.getIsEmergency()) && rr.getId() != null && rr.getSourceTeamId() != null) {
                notificationService.markEmergencyReassigned(coordinatorId, rr.getId(), team.getName());
            }
        }

        return mapper.toResponseWithDetails(group, links, allAssignments, timeline);
    }

    private void syncAssignedRequestStatuses(List<RescueRequestEntity> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        for (RescueRequestEntity rr : requests) {
            rr.setStatus(RescueRequestStatus.ASSIGNED);
        }
        rescueRequestRepository.saveAll(requests);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponse getLatestActiveAssignment(Long taskGroupId) {
        List<RescueAssignmentEntity> active =
                rescueAssignmentRepository.findByTaskGroupIdAndIsActiveTrue(taskGroupId);
        if (active.isEmpty()) {
            throw new NotFoundException("Không có phân công nào đang hoạt động cho nhóm nhiệm vụ này");
        }

        // Giả sử chỉ có 1 active assignment
        RescueAssignmentEntity a = active.get(0);

        AssignmentResponse.AssignmentResponseBuilder builder = AssignmentResponse.builder()
                .id(a.getId())
                .taskGroupId(a.getTaskGroup().getId())
                .taskGroupCode(a.getTaskGroup().getCode())
                .teamId(a.getTeam().getId())
                .teamName(a.getTeam().getName())
                .assignedAt(a.getAssignedAt())
                .active(a.getIsActive());

        if (a.getAsset() != null) {
            builder.assetId(a.getAsset().getId())
                    .assetCode(a.getAsset().getCode())
                    .assetName(a.getAsset().getName());
        }

        if (a.getAssignedBy() != null) {
            builder.assignedById(a.getAssignedBy().getId())
                    .assignedByName(a.getAssignedBy().getFullName());
        }

        return builder.build();
    }
}
