package com.floodrescue.module.notification.service;

import com.floodrescue.module.notification.dto.EmergencyAckResponse;
import com.floodrescue.module.notification.dto.NotificationResponse;
import com.floodrescue.module.notification.entity.NotificationEntity;
import com.floodrescue.module.notification.repository.NotificationRepository;
import com.floodrescue.module.rescue.entity.RescueRequestAttachmentEntity;
import com.floodrescue.module.rescue.entity.RescueRequestEntity;
import com.floodrescue.module.rescue.entity.RescueRequestTimelineEntity;
import com.floodrescue.module.rescue.entity.TaskGroupEntity;
import com.floodrescue.module.rescue.entity.TaskGroupRequestEntity;
import com.floodrescue.module.rescue.repository.RescueAttachmentRepository;
import com.floodrescue.module.rescue.repository.RescueRequestRepository;
import com.floodrescue.module.rescue.repository.RescueTimelineRepository;
import com.floodrescue.module.rescue.repository.TaskGroupRepository;
import com.floodrescue.module.rescue.repository.TaskGroupRequestRepository;
import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.module.user.repository.UserRepository;
import com.floodrescue.shared.enums.RescuePriority;
import com.floodrescue.shared.enums.RescueRequestStatus;
import com.floodrescue.shared.enums.TimelineEventType;
import com.floodrescue.shared.exception.BusinessException;
import com.floodrescue.shared.exception.NotFoundException;
import com.floodrescue.shared.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String EVENT_RESCUER_EMERGENCY = "RESCUER_EMERGENCY";
    private static final String REF_TASK_GROUP = "TASK_GROUP";
    private static final String REF_RESCUE_REQUEST = "RESCUE_REQUEST";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TaskGroupRepository taskGroupRepository;
    private final TaskGroupRequestRepository taskGroupRequestRepository;
    private final RescueRequestRepository rescueRequestRepository;
    private final RescueAttachmentRepository rescueAttachmentRepository;
    private final RescueTimelineRepository rescueTimelineRepository;

    @Override
    @Transactional
    public void notifyRole(
            String roleCode,
            String title,
            String content,
            String eventCode,
            String referenceType,
            Long referenceId,
            boolean urgent,
            Long sourceTeamId
    ) {
        List<UserEntity> targets = userRepository.findAllByRoleCode(roleCode);
        List<Long> userIds = targets.stream().map(UserEntity::getId).toList();
        notifyUsers(userIds, title, content, eventCode, referenceType, referenceId, urgent, sourceTeamId);
    }

    @Override
    @Transactional
    public void notifyUsers(
            List<Long> userIds,
            String title,
            String content,
            String eventCode,
            String referenceType,
            Long referenceId,
            boolean urgent,
            Long sourceTeamId
    ) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (Long userId : new LinkedHashSet<>(userIds)) {
            if (userId == null) {
                continue;
            }
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại: " + userId));

            NotificationEntity n = notificationRepository
                    .findFirstByUserIdAndEventCodeAndReferenceTypeAndReferenceIdOrderByIdDesc(
                            userId,
                            eventCode,
                            referenceType,
                            referenceId
                    )
                    .orElseGet(() -> NotificationEntity.builder()
                            .user(user)
                            .eventCode(eventCode)
                            .referenceType(referenceType)
                            .referenceId(referenceId)
                            .build());

            n.setTitle(title);
            n.setContent(content);
            n.setIsRead(false);
            n.setIsUrgent(urgent);
            n.setAcknowledgedAt(null);
            if (sourceTeamId != null) {
                n.setSourceTeamId(sourceTeamId);
            }
            notificationRepository.save(n);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Long userId, boolean unreadOnly, Pageable pageable) {
        Page<NotificationEntity> page = unreadOnly
                ? notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        NotificationEntity n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Thông báo không tồn tại"));
        if (!n.getUser().getId().equals(userId)) {
            throw new NotFoundException("Không tìm thấy thông báo");
        }

        if (!Boolean.TRUE.equals(n.getIsRead())) {
            n.setIsRead(true);
            n.setAcknowledgedAt(LocalDateTime.now());
            if (EVENT_RESCUER_EMERGENCY.equalsIgnoreCase(n.getEventCode()) && !"QUEUED".equalsIgnoreCase(n.getActionStatus())) {
                n.setActionStatus("VIEWED");
            }
            notificationRepository.save(n);

            if (EVENT_RESCUER_EMERGENCY.equalsIgnoreCase(n.getEventCode())
                    && REF_TASK_GROUP.equalsIgnoreCase(n.getReferenceType())
                    && n.getReferenceId() != null) {
                notifyEmergencyViewedToRescuer(n);
            }
        }
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        Page<NotificationEntity> page = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(
                userId, Pageable.ofSize(200)
        );
        for (NotificationEntity n : page.getContent()) {
            n.setIsRead(true);
            n.setAcknowledgedAt(LocalDateTime.now());
            if (EVENT_RESCUER_EMERGENCY.equalsIgnoreCase(n.getEventCode()) && !"QUEUED".equalsIgnoreCase(n.getActionStatus())) {
                n.setActionStatus("VIEWED");
            }
        }
        notificationRepository.saveAll(page.getContent());
    }

    @Override
    @Transactional
    public Long queueEmergencyRequest(Long userId, Long notificationId, boolean direct, String note) {
        NotificationEntity notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Thông báo không tồn tại"));
        if (!notif.getUser().getId().equals(userId)) {
            throw new NotFoundException("Không tìm thấy thông báo");
        }
        if (!EVENT_RESCUER_EMERGENCY.equalsIgnoreCase(notif.getEventCode())
                || !REF_TASK_GROUP.equalsIgnoreCase(notif.getReferenceType())
                || notif.getReferenceId() == null) {
            throw new BusinessException("Thông báo này không hỗ trợ đưa vào hàng đợi");
        }

        TaskGroupEntity taskGroup = taskGroupRepository.findById(notif.getReferenceId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhiệm vụ cần xử lý khẩn cấp"));

        List<TaskGroupRequestEntity> links = taskGroupRequestRepository.findByTaskGroupId(taskGroup.getId());
        if (links.isEmpty()) {
            throw new BusinessException("Nhiệm vụ không có yêu cầu gốc để đưa vào hàng đợi");
        }

        RescueRequestEntity source = rescueRequestRepository.findById(links.get(0).getRescueRequest().getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu cứu hộ gốc"));

        Long sourceTeamId = taskGroup.getAssignedTeam() != null ? taskGroup.getAssignedTeam().getId() : notif.getSourceTeamId();
        int emergencyNo = (int) (rescueRequestRepository.countByEmergencyParentRequestId(source.getId()) + 1);

        RescueRequestEntity queued = RescueRequestEntity.builder()
                .code(generateUniqueRequestCode())
                .citizen(source.getCitizen())
                .status(RescueRequestStatus.PENDING)
                .priority(RescuePriority.HIGH)
                .masterRequest(source.getMasterRequest())
                .affectedPeopleCount(source.getAffectedPeopleCount())
                .description(mergeEmergencyDescription(source.getDescription(), note, taskGroup.getCode(), emergencyNo))
                .addressText(source.getAddressText())
                .latitude(source.getLatitude())
                .longitude(source.getLongitude())
                .locationDescription(source.getLocationDescription())
                .locationVerified(source.getLocationVerified())
                .isEmergency(true)
                .emergencyNo(emergencyNo)
                .sourceTeamId(sourceTeamId)
                .emergencyParentRequestId(source.getId())
                .build();

        queued = rescueRequestRepository.save(queued);

        List<RescueRequestAttachmentEntity> sourceAttachments = rescueAttachmentRepository.findByRescueRequestId(source.getId());
        if (!sourceAttachments.isEmpty()) {
            List<RescueRequestAttachmentEntity> cloned = new ArrayList<>();
            for (RescueRequestAttachmentEntity a : sourceAttachments) {
                cloned.add(RescueRequestAttachmentEntity.builder()
                        .rescueRequest(queued)
                        .fileUrl(a.getFileUrl())
                        .fileType(a.getFileType())
                        .build());
            }
            rescueAttachmentRepository.saveAll(cloned);
        }

        UserEntity actor = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        rescueTimelineRepository.save(RescueRequestTimelineEntity.builder()
                .rescueRequest(queued)
                .actor(actor)
                .eventType(TimelineEventType.STATUS_CHANGE)
                .fromStatus(null)
                .toStatus(RescueRequestStatus.PENDING)
                .note("Yêu cầu khẩn cấp #" + emergencyNo + " được đưa vào hàng đợi từ nhiệm vụ " + taskGroup.getCode())
                .build());

        if (!direct && note != null && !note.isBlank()) {
            rescueTimelineRepository.save(RescueRequestTimelineEntity.builder()
                    .rescueRequest(queued)
                    .actor(actor)
                    .eventType(TimelineEventType.NOTE)
                    .note("Điều phối bổ sung: " + note.trim())
                    .build());
        }

        notif.setIsRead(true);
        notif.setAcknowledgedAt(LocalDateTime.now());
        notif.setActionStatus("QUEUED");
        notif.setActionNote((note == null || note.isBlank()) ? null : note.trim());
        notif.setQueueRequestId(queued.getId());
        if (sourceTeamId != null) {
            notif.setSourceTeamId(sourceTeamId);
        }
        notificationRepository.save(notif);

        notifyRescuerByTeam(
                sourceTeamId,
                "Điều phối đã đưa yêu cầu khẩn cấp vào hàng đợi",
                "Yêu cầu khẩn cấp #" + emergencyNo + " đã vào hàng đợi (" + queued.getCode() + ").",
                "EMERGENCY_QUEUED",
                REF_RESCUE_REQUEST,
                queued.getId(),
                sourceTeamId
        );

        return queued.getId();
    }

    @Override
    @Transactional
    public void markEmergencyConfirmed(Long coordinatorId, Long queueRequestId) {
        List<NotificationEntity> items = notificationRepository.findByEventCodeAndQueueRequestId(EVENT_RESCUER_EMERGENCY, queueRequestId);
        if (items.isEmpty()) {
            return;
        }

        UserEntity coordinator = userRepository.findById(coordinatorId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));

        for (NotificationEntity n : items) {
            n.setActionStatus("CONFIRMED");
            n.setActionNote("Điều phối " + coordinator.getFullName() + " đã xác nhận yêu cầu khẩn cấp.");
        }
        notificationRepository.saveAll(items);

        NotificationEntity sample = items.get(0);
        notifyRescuerByTeam(
                sample.getSourceTeamId(),
                "Yêu cầu khẩn cấp đã được xác nhận",
                "Điều phối đã xác nhận yêu cầu " + queueRequestId + " từ cảnh báo khẩn cấp của đội.",
                "EMERGENCY_CONFIRMED",
                REF_RESCUE_REQUEST,
                queueRequestId,
                sample.getSourceTeamId()
        );
    }

    @Override
    @Transactional
    public void markEmergencyReassigned(Long coordinatorId, Long queueRequestId, String assignedTeamName) {
        List<NotificationEntity> items = notificationRepository.findByEventCodeAndQueueRequestId(EVENT_RESCUER_EMERGENCY, queueRequestId);
        if (items.isEmpty()) {
            return;
        }

        UserEntity coordinator = userRepository.findById(coordinatorId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));

        for (NotificationEntity n : items) {
            n.setActionStatus("REASSIGNED");
            n.setActionNote("Điều phối " + coordinator.getFullName() + " đã phân công đội " + (assignedTeamName != null ? assignedTeamName : "khác") + ".");
        }
        notificationRepository.saveAll(items);

        NotificationEntity sample = items.get(0);
        notifyRescuerByTeam(
                sample.getSourceTeamId(),
                "Yêu cầu khẩn cấp đã được điều phối đội khác",
                "Điều phối đã phân công đội " + (assignedTeamName != null ? assignedTeamName : "khác") + " hỗ trợ yêu cầu khẩn cấp.",
                "EMERGENCY_REASSIGNED",
                REF_RESCUE_REQUEST,
                queueRequestId,
                sample.getSourceTeamId()
        );
    }

    @Override
    @Transactional
    public void markEmergencyOverloaded(Long coordinatorId, Long queueRequestId, String note) {
        if (queueRequestId == null) {
            throw new BusinessException("Thiếu queueRequestId để báo quá tải");
        }

        List<NotificationEntity> items = notificationRepository.findByEventCodeAndQueueRequestId(EVENT_RESCUER_EMERGENCY, queueRequestId);
        if (items.isEmpty()) {
            throw new BusinessException("Không tìm thấy cảnh báo khẩn cấp tương ứng để cập nhật quá tải");
        }

        UserEntity coordinator = userRepository.findById(coordinatorId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        String detail = note == null ? "" : note.trim();
        String actionNote = "Điều phối " + coordinator.getFullName() + " báo quá tải. "
                + (detail.isBlank() ? "Hiện không còn đội rảnh." : detail);

        for (NotificationEntity n : items) {
            n.setIsRead(true);
            n.setAcknowledgedAt(LocalDateTime.now());
            n.setActionStatus("WAITING_OVERLOAD");
            n.setActionNote(actionNote);
        }
        notificationRepository.saveAll(items);

        RescueRequestEntity queueRequest = rescueRequestRepository.findById(queueRequestId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu khẩn cấp trong hàng đợi"));

        rescueTimelineRepository.save(RescueRequestTimelineEntity.builder()
                .rescueRequest(queueRequest)
                .actor(coordinator)
                .eventType(TimelineEventType.NOTE)
                .note("Điều phối báo quá tải, yêu cầu tiếp tục ở trạng thái đang đợi. "
                        + (detail.isBlank() ? "Không còn đội rảnh." : detail))
                .build());

        NotificationEntity sample = items.get(0);
        notifyRescuerByTeam(
                sample.getSourceTeamId(),
                "Điều phối báo quá tải cho yêu cầu khẩn cấp",
                "Hiện không còn đội nào rảnh. Đội đang xử lý hiện trường hoặc chờ điều phối bổ sung."
                        + (detail.isBlank() ? "" : " Mô tả: " + detail),
                "EMERGENCY_OVERLOADED",
                REF_RESCUE_REQUEST,
                queueRequestId,
                sample.getSourceTeamId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyAckResponse> getCoordinatorEmergencyAcks(Long taskGroupId) {
        List<NotificationEntity> items = notificationRepository.findCoordinatorNotificationsByRef(
                EVENT_RESCUER_EMERGENCY,
                REF_TASK_GROUP,
                taskGroupId
        );
        return items.stream()
                .map(n -> EmergencyAckResponse.builder()
                        .coordinatorId(n.getUser().getId())
                        .coordinatorName(n.getUser().getFullName())
                        .read(Boolean.TRUE.equals(n.getIsRead()))
                        .actionStatus(n.getActionStatus())
                        .actionNote(n.getActionNote())
                        .queueRequestId(n.getQueueRequestId())
                        .acknowledgedAt(n.getAcknowledgedAt())
                        .build())
                .toList();
    }

    private void notifyEmergencyViewedToRescuer(NotificationEntity coordinatorNotif) {
        Long sourceTeamId = coordinatorNotif.getSourceTeamId();
        if (sourceTeamId == null && coordinatorNotif.getReferenceId() != null) {
            TaskGroupEntity group = taskGroupRepository.findById(coordinatorNotif.getReferenceId()).orElse(null);
            if (group != null && group.getAssignedTeam() != null) {
                sourceTeamId = group.getAssignedTeam().getId();
            }
        }

        notifyRescuerByTeam(
                sourceTeamId,
                "Điều phối đã xem yêu cầu khẩn cấp",
                "Điều phối đã xác nhận đã xem cảnh báo khẩn cấp của đội cho nhiệm vụ #" + coordinatorNotif.getReferenceId() + ".",
                "EMERGENCY_VIEWED",
                REF_TASK_GROUP,
                coordinatorNotif.getReferenceId(),
                sourceTeamId
        );
    }

    private void notifyRescuerByTeam(
            Long teamId,
            String title,
            String content,
            String eventCode,
            String referenceType,
            Long refId,
            Long sourceTeamId
    ) {
        if (teamId == null) {
            return;
        }
        List<Long> rescuerIds = userRepository.findByTeamId(teamId).stream()
                .filter(u -> u.getRole() != null && "RESCUER".equalsIgnoreCase(u.getRole().getCode()))
                .map(UserEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        notifyUsers(rescuerIds, title, content, eventCode, referenceType, refId, true, sourceTeamId);
    }

    private NotificationResponse toResponse(NotificationEntity n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .content(n.getContent())
                .read(n.getIsRead())
                .urgent(n.getIsUrgent())
                .eventCode(n.getEventCode())
                .referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId())
                .actionStatus(n.getActionStatus())
                .actionNote(n.getActionNote())
                .queueRequestId(n.getQueueRequestId())
                .sourceTeamId(n.getSourceTeamId())
                .acknowledgedAt(n.getAcknowledgedAt())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private String generateUniqueRequestCode() {
        String code;
        int attempts = 0;
        do {
            code = CodeGenerator.generateRescueRequestCode();
            attempts++;
            if (attempts > 10) {
                throw new BusinessException("Không thể tạo mã yêu cầu cứu hộ duy nhất");
            }
        } while (rescueRequestRepository.existsByCode(code));
        return code;
    }

    private String mergeEmergencyDescription(String oldDescription, String note, String taskGroupCode, int emergencyNo) {
        StringBuilder builder = new StringBuilder();
        if (oldDescription != null && !oldDescription.isBlank()) {
            builder.append(oldDescription.trim());
        }
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append("[YÊU CẦU KHẨN CẤP #")
                .append(emergencyNo)
                .append("] Từ nhiệm vụ ")
                .append(taskGroupCode != null ? taskGroupCode : "không rõ");
        if (note != null && !note.isBlank()) {
            builder.append(" | Mô tả thêm: ").append(note.trim());
        }
        return builder.toString();
    }
}
