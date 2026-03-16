package com.floodrescue.module.rescue.service;

import com.floodrescue.module.rescue.dto.request.CreateTaskGroupRequest;
import com.floodrescue.module.rescue.dto.response.TaskGroupResponse;
import com.floodrescue.module.rescue.entity.RescueRequestEntity;
import com.floodrescue.module.rescue.entity.TaskGroupEntity;
import com.floodrescue.module.rescue.entity.TaskGroupRequestEntity;
import com.floodrescue.module.rescue.entity.TaskGroupTimelineEntity;
import com.floodrescue.module.rescue.mapper.TaskGroupMapper;
import com.floodrescue.module.rescue.repository.RescueRequestRepository;
import com.floodrescue.module.rescue.repository.RescueAssignmentRepository;
import com.floodrescue.module.rescue.repository.TaskGroupRepository;
import com.floodrescue.module.rescue.repository.TaskGroupRequestRepository;
import com.floodrescue.module.rescue.repository.TaskGroupTimelineRepository;
import com.floodrescue.module.team.entity.TeamEntity;
import com.floodrescue.module.team.repository.TeamRepository;
import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.module.user.repository.UserRepository;
import com.floodrescue.shared.enums.RescueRequestStatus;
import com.floodrescue.shared.enums.TaskGroupStatus;
import com.floodrescue.shared.exception.BusinessException;
import com.floodrescue.shared.exception.NotFoundException;
import com.floodrescue.shared.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskGroupServiceImpl implements TaskGroupService {

    private final TaskGroupRepository taskGroupRepository;
    private final TaskGroupRequestRepository taskGroupRequestRepository;
    private final TaskGroupTimelineRepository taskGroupTimelineRepository;
    private final RescueAssignmentRepository rescueAssignmentRepository;
    private final RescueRequestRepository rescueRequestRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TaskGroupMapper mapper;

    @Override
    @Transactional
    public TaskGroupResponse createTaskGroup(CreateTaskGroupRequest request, Long coordinatorId) {
        if (request.getRescueRequestIds() == null || request.getRescueRequestIds().isEmpty()) {
            throw new BusinessException("Danh sách yêu cầu cứu hộ không được để trống");
        }

        // 1 nhiệm vụ (rescue_request) chỉ thuộc 1 task group để tránh tạo trùng khi bấm phân công nhiều lần.
        if (request.getRescueRequestIds().size() == 1) {
            Long requestId = request.getRescueRequestIds().get(0);
            var existing = taskGroupRequestRepository.findByRescueRequestId(requestId).stream()
                    .map(TaskGroupRequestEntity::getTaskGroup)
                    .filter(java.util.Objects::nonNull)
                    .max(Comparator.comparing(TaskGroupEntity::getId));
            if (existing.isPresent()) {
                return mapper.toResponse(existing.get());
            }
        } else {
            for (Long requestId : request.getRescueRequestIds()) {
                var existingLinks = taskGroupRequestRepository.findByRescueRequestId(requestId);
                if (!existingLinks.isEmpty()) {
                    throw new BusinessException("Yêu cầu cứu hộ " + requestId + " đã thuộc một nhóm nhiệm vụ.");
                }
            }
        }

        UserEntity coordinator = userRepository.findById(coordinatorId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));

        // Generate group code (reuse rescue code generator with different prefix if needed)
        String code = CodeGenerator.generateRescueRequestCode().replaceFirst("RR", "TG");

        TaskGroupEntity.TaskGroupEntityBuilder builder = TaskGroupEntity.builder()
                .code(code)
                .status(TaskGroupStatus.NEW)
                .note(request.getNote())
                .createdBy(coordinator);

        if (request.getAssignedTeamId() != null) {
            TeamEntity team = teamRepository.findById(request.getAssignedTeamId())
                    .orElseThrow(() -> new NotFoundException("Đội cứu hộ không tồn tại"));
            builder.assignedTeam(team);
        }

        TaskGroupEntity group = taskGroupRepository.save(builder.build());

        // Link rescue requests
        List<TaskGroupRequestEntity> links = request.getRescueRequestIds().stream()
                .map(reqId -> {
                    var rescueRequest = rescueRequestRepository.findById(reqId)
                            .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại: " + reqId));

                    TaskGroupRequestEntity.TaskGroupRequestId id =
                            new TaskGroupRequestEntity.TaskGroupRequestId(group.getId(), rescueRequest.getId());

                    return TaskGroupRequestEntity.builder()
                            .id(id)
                            .taskGroup(group)
                            .rescueRequest(rescueRequest)
                            .build();
                })
                .toList();

        taskGroupRequestRepository.saveAll(links);

        // Timeline
        createTimeline(group, coordinator, "CREATE", request.getNote());

        return mapper.toResponse(group);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskGroupResponse getTaskGroup(Long id) {
        TaskGroupEntity group = taskGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nhóm nhiệm vụ không tồn tại"));

        var requests = taskGroupRequestRepository.findByTaskGroupId(id);
        var assignments = rescueAssignmentRepository.findByTaskGroupIdAndIsActiveTrue(id);
        var timeline = taskGroupTimelineRepository.findByTaskGroupIdOrderByCreatedAtDesc(id);

        return mapper.toResponseWithDetails(group, requests, assignments, timeline);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskGroupResponse> getTaskGroups(TaskGroupStatus status, Pageable pageable) {
        Page<TaskGroupEntity> page;
        if (status != null) {
            page = taskGroupRepository.findByStatus(status, pageable);
        } else {
            page = taskGroupRepository.findAll(pageable);
        }
        return page.map(mapper::toResponse);
    }

    @Override
    @Transactional
    public TaskGroupResponse changeStatus(Long id, TaskGroupStatus status, String note, Long coordinatorId) {
        if (status == TaskGroupStatus.IN_PROGRESS || status == TaskGroupStatus.DONE) {
            throw new BusinessException("Điều phối viên không có quyền đổi trạng thái này. Đội cứu hộ sẽ cập nhật tiến độ.");
        }

        TaskGroupEntity group = taskGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nhóm nhiệm vụ không tồn tại"));

        UserEntity coordinator = userRepository.findById(coordinatorId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));

        group.setStatus(status);
        group = taskGroupRepository.save(group);
        syncLinkedRescueRequestStatus(group.getId(), status);

        createTimeline(group, coordinator, "STATUS_CHANGE", note);

        var requests = taskGroupRequestRepository.findByTaskGroupId(id);
        var timeline = taskGroupTimelineRepository.findByTaskGroupIdOrderByCreatedAtDesc(id);

        return mapper.toResponseWithDetails(group, requests, List.of(), timeline);
    }

    private void syncLinkedRescueRequestStatus(Long taskGroupId, TaskGroupStatus taskStatus) {
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

    private void createTimeline(TaskGroupEntity group, UserEntity actor, String eventType, String note) {
        TaskGroupTimelineEntity tl = TaskGroupTimelineEntity.builder()
                .taskGroup(group)
                .actor(actor)
                .eventType(eventType)
                .note(note)
                .build();
        taskGroupTimelineRepository.save(tl);
    }
}
