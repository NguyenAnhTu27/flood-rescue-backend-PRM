package com.floodrescue.module.rescue.service;

import com.floodrescue.module.rescue.dto.response.RescueChatMessageResponse;
import com.floodrescue.module.rescue.entity.RescueRequestEntity;
import com.floodrescue.module.rescue.entity.RescueRequestTimelineEntity;
import com.floodrescue.module.rescue.entity.TaskGroupRequestEntity;
import com.floodrescue.module.rescue.repository.RescueRequestRepository;
import com.floodrescue.module.rescue.repository.RescueTimelineRepository;
import com.floodrescue.module.rescue.repository.TaskGroupRequestRepository;
import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.module.user.repository.UserRepository;
import com.floodrescue.shared.enums.TimelineEventType;
import com.floodrescue.shared.exception.BusinessException;
import com.floodrescue.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RescueChatServiceImpl implements RescueChatService {

    private final RescueRequestRepository rescueRequestRepository;
    private final RescueTimelineRepository rescueTimelineRepository;
    private final TaskGroupRequestRepository taskGroupRequestRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RescueChatMessageResponse> getMessages(Long rescueRequestId, Long userId) {
        UserEntity user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        RescueRequestEntity rescueRequest = rescueRequestRepository.findById(rescueRequestId)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại"));

        validateAccess(user, rescueRequest);

        return rescueTimelineRepository
                .findByRescueRequestIdAndEventTypeOrderByCreatedAtAsc(rescueRequestId, TimelineEventType.NOTE)
                .stream()
                .filter(item -> item.getNote() != null && !item.getNote().trim().isEmpty())
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RescueChatMessageResponse sendMessage(Long rescueRequestId, Long userId, String message) {
        UserEntity user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        RescueRequestEntity rescueRequest = rescueRequestRepository.findById(rescueRequestId)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại"));

        validateAccess(user, rescueRequest);

        String normalizedMessage = normalizeRequired(message, "Tin nhắn không được để trống");

        RescueRequestTimelineEntity saved = rescueTimelineRepository.save(
                RescueRequestTimelineEntity.builder()
                        .rescueRequest(rescueRequest)
                        .actor(user)
                        .eventType(TimelineEventType.NOTE)
                        .note(normalizedMessage)
                        .build()
        );

        return toResponse(saved);
    }

    private void validateAccess(UserEntity user, RescueRequestEntity rescueRequest) {
        String roleCode = user.getRole() != null && user.getRole().getCode() != null
                ? user.getRole().getCode().toUpperCase(Locale.ROOT)
                : "";

        switch (roleCode) {
            case "ADMIN":
            case "MANAGER":
            case "COORDINATOR":
                return;
            case "CITIZEN":
                if (!rescueRequest.getCitizen().getId().equals(user.getId())) {
                    throw new BusinessException("Bạn không có quyền truy cập cuộc trò chuyện của yêu cầu này");
                }
                return;
            case "RESCUER":
                Long userTeamId = user.getTeamId();
                if (userTeamId == null) {
                    throw new BusinessException("Tài khoản đội cứu hộ chưa được gán vào đội");
                }
                List<TaskGroupRequestEntity> requestLinks = taskGroupRequestRepository.findByRescueRequestId(rescueRequest.getId());
                boolean belongsToAssignedTeam = requestLinks.stream()
                        .filter(link -> link.getTaskGroup() != null && link.getTaskGroup().getAssignedTeam() != null)
                        .max(Comparator.comparing(link -> link.getTaskGroup().getId()))
                        .map(link -> userTeamId.equals(link.getTaskGroup().getAssignedTeam().getId()))
                        .orElse(false);
                if (!belongsToAssignedTeam) {
                    throw new BusinessException("Bạn không có quyền truy cập cuộc trò chuyện của yêu cầu này");
                }
                return;
            default:
                throw new BusinessException("Vai trò hiện tại không được hỗ trợ truy cập chat");
        }
    }

    private RescueChatMessageResponse toResponse(RescueRequestTimelineEntity entity) {
        UserEntity actor = entity.getActor();
        return RescueChatMessageResponse.builder()
                .id(entity.getId())
                .rescueRequestId(entity.getRescueRequest().getId())
                .senderId(actor != null ? actor.getId() : null)
                .senderName(actor != null ? actor.getFullName() : null)
                .senderRole(actor != null && actor.getRole() != null ? actor.getRole().getCode() : null)
                .message(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }
}
