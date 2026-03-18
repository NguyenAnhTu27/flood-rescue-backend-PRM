package com.floodrescue.module.rescue.service;

import com.floodrescue.module.notification.entity.NotificationEntity;
import com.floodrescue.module.notification.repository.NotificationRepository;
import com.floodrescue.module.rescue.dto.request.*;
import com.floodrescue.module.rescue.dto.response.CitizenRescueConfirmationResponse;
import com.floodrescue.module.rescue.dto.response.BlockedCitizenResponse;
import com.floodrescue.module.rescue.dto.response.RescueRequestResponse;
import com.floodrescue.module.rescue.entity.RescueRequestAttachmentEntity;
import com.floodrescue.module.rescue.entity.RescueRequestEntity;
import com.floodrescue.module.rescue.entity.RescueRequestTimelineEntity;
import com.floodrescue.module.rescue.entity.TaskGroupRequestEntity;
import com.floodrescue.module.rescue.mapper.RescueRequestMapper;
import com.floodrescue.module.rescue.repository.RescueAttachmentRepository;
import com.floodrescue.module.rescue.repository.RescueRequestRepository;
import com.floodrescue.module.rescue.repository.RescueTimelineRepository;
import com.floodrescue.module.rescue.repository.TaskGroupRequestRepository;
import com.floodrescue.module.notification.service.NotificationService;
import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.module.user.repository.UserRepository;
import com.floodrescue.shared.enums.RescuePriority;
import com.floodrescue.shared.enums.RescueRequestStatus;
import com.floodrescue.shared.enums.TaskGroupStatus;
import com.floodrescue.shared.enums.TimelineEventType;
import com.floodrescue.shared.exception.BusinessException;
import com.floodrescue.shared.exception.NotFoundException;
import com.floodrescue.shared.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RescueRequestServiceImpl implements RescueRequestService {

    private final RescueRequestRepository rescueRequestRepository;
    private final RescueAttachmentRepository attachmentRepository;
    private final RescueTimelineRepository timelineRepository;
    private final UserRepository userRepository;
    private final RescueRequestMapper mapper;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final TaskGroupRequestRepository taskGroupRequestRepository;

    @Override
    @Transactional
    public RescueRequestResponse createRescueRequest(Long citizenId, RescueRequestCreateRequest request) {
        UserEntity citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        validateCitizenCanCreateRequest(citizen);

        String code = generateUniqueRescueRequestCode();

        RescueRequestEntity entity = RescueRequestEntity.builder()
                .code(code)
                .citizen(citizen)
                .status(RescueRequestStatus.PENDING)
                .priority(request.getPriority())
                .affectedPeopleCount(request.getAffectedPeopleCount())
                .description(request.getDescription())
                .addressText(request.getAddressText())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationDescription(request.getLocationDescription())
                .locationVerified(false)
                .build();

        final RescueRequestEntity savedEntity = rescueRequestRepository.save(entity);

        // Save attachments if any
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            List<RescueRequestAttachmentEntity> attachments = request.getAttachments().stream()
                    .map(att -> RescueRequestAttachmentEntity.builder()
                            .rescueRequest(savedEntity)
                            .fileUrl(att.getFileUrl())
                            .fileType(att.getFileType())
                            .build())
                    .collect(Collectors.toList());
            attachmentRepository.saveAll(attachments);
        }

        // Create initial timeline entry
        createTimelineEntry(savedEntity, citizen, TimelineEventType.STATUS_CHANGE,
                null, RescueRequestStatus.PENDING, "Yêu cầu cứu hộ được tạo");

        notificationService.notifyRole(
                "COORDINATOR",
                "Yêu cầu cứu hộ mới",
                "Citizen vừa tạo yêu cầu " + savedEntity.getCode() + " tại " + (savedEntity.getAddressText() != null ? savedEntity.getAddressText() : "địa chỉ chưa rõ"),
                "CITIZEN_NEW_REQUEST",
                "RESCUE_REQUEST",
                savedEntity.getId(),
                true,
                null
        );

        return enrichEmergencyActionStatus(mapper.toResponse(savedEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public RescueRequestResponse getRescueRequestById(Long id) {
        RescueRequestEntity entity = rescueRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại"));
        entity = reconcileStatusFromLatestTaskGroup(entity);

        List<RescueRequestAttachmentEntity> attachments = attachmentRepository.findByRescueRequestId(id);
        List<RescueRequestTimelineEntity> timeline = timelineRepository.findByRescueRequestIdOrderByCreatedAtDesc(id);

        return enrichEmergencyActionStatus(mapper.toResponseWithDetails(entity, attachments, timeline));
    }

    @Override
    @Transactional(readOnly = true)
    public RescueRequestResponse getRescueRequestByCode(String code) {
        RescueRequestEntity entity = rescueRequestRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại"));
        entity = reconcileStatusFromLatestTaskGroup(entity);

        List<RescueRequestAttachmentEntity> attachments = attachmentRepository.findByRescueRequestId(entity.getId());
        List<RescueRequestTimelineEntity> timeline = timelineRepository
                .findByRescueRequestIdOrderByCreatedAtDesc(entity.getId());

        return enrichEmergencyActionStatus(mapper.toResponseWithDetails(entity, attachments, timeline));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RescueRequestResponse> getRescueRequestsByCitizen(Long citizenId, Pageable pageable) {
        Pageable normalized = pageable;
        if (pageable == null) {
            normalized = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        } else if (pageable.getSort().isUnsorted()) {
            normalized = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }
        Page<RescueRequestEntity> entities = rescueRequestRepository.findByCitizenId(citizenId, normalized);
        reconcileStatusFromLatestTaskGroup(entities.getContent());
        return enrichEmergencyActionStatusPage(entities.map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RescueRequestResponse> getRescueRequestsByStatus(RescueRequestStatus status, Pageable pageable) {
        Page<RescueRequestEntity> entities = rescueRequestRepository.findByStatus(status, pageable);
        return enrichEmergencyActionStatusPage(entities.map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RescueRequestResponse> getPendingRescueRequests(Pageable pageable) {
        Page<RescueRequestEntity> entities = rescueRequestRepository.findPendingRequestsOrderedByPriority(
                RescueRequestStatus.PENDING, pageable);
        return enrichEmergencyActionStatusPage(entities.map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RescueRequestResponse> searchRescueRequests(RescueRequestStatus status, RescuePriority priority,
            String keyword, Pageable pageable) {
        Specification<RescueRequestEntity> spec = (root, query, cb) -> cb.conjunction();

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String likePattern = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("code")), likePattern),
                    cb.like(cb.lower(root.get("addressText")), likePattern)));
        }

        Page<RescueRequestEntity> entities = rescueRequestRepository.findAll(spec, pageable);
        return enrichEmergencyActionStatusPage(entities.map(mapper::toResponse));
    }

    @Override
    @Transactional
    public RescueRequestResponse updateRescueRequest(Long id, Long citizenId, RescueRequestUpdateRequest request) {
        RescueRequestEntity entity = rescueRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại"));

        // Check ownership
        if (!entity.getCitizen().getId().equals(citizenId)) {
            throw new BusinessException("Bạn không có quyền chỉnh sửa yêu cầu cứu hộ này");
        }

        // Check if can be updated
        if (entity.getStatus() == RescueRequestStatus.COMPLETED ||
                entity.getStatus() == RescueRequestStatus.CANCELLED ||
                entity.getStatus() == RescueRequestStatus.DUPLICATE) {
            throw new BusinessException("Không thể chỉnh sửa yêu cầu cứu hộ ở trạng thái này");
        }

        // Update fields
        if (request.getAffectedPeopleCount() != null) {
            entity.setAffectedPeopleCount(request.getAffectedPeopleCount());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getAddressText() != null) {
            entity.setAddressText(request.getAddressText());
        }
        if (request.getPriority() != null) {
            entity.setPriority(request.getPriority());
        }

        entity = rescueRequestRepository.save(entity);

        if (request.getAttachments() != null) {
            attachmentRepository.deleteByRescueRequestId(entity.getId());
            saveAttachments(entity, request.getAttachments());
        }

        // Add timeline entry
        UserEntity user = userRepository.findById(citizenId).orElseThrow();
        createTimelineEntry(entity, user, TimelineEventType.NOTE,
                null, null, "Yêu cầu cứu hộ được cập nhật");

        notificationService.notifyRole(
                "COORDINATOR",
                "Citizen vừa cập nhật yêu cầu cứu hộ",
                "Yêu cầu " + entity.getCode() + " vừa được cập nhật bởi citizen.",
                "CITIZEN_UPDATE_REQUEST",
                "RESCUE_REQUEST",
                entity.getId(),
                true,
                null
        );

        List<RescueRequestAttachmentEntity> attachments = attachmentRepository.findByRescueRequestId(entity.getId());
        List<RescueRequestTimelineEntity> timeline = timelineRepository.findByRescueRequestIdOrderByCreatedAtDesc(entity.getId());
        return enrichEmergencyActionStatus(mapper.toResponseWithDetails(entity, attachments, timeline));
    }

    @Override
    @Transactional
    public RescueRequestResponse verifyRescueRequest(Long id, Long coordinatorId, VerifyRequest request) {
        RescueRequestEntity entity = rescueRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại"));

        if (entity.getStatus() != RescueRequestStatus.PENDING) {
            throw new BusinessException("Chỉ có thể xác minh yêu cầu ở trạng thái PENDING");
        }

        UserEntity coordinator = userRepository.findById(coordinatorId).orElseThrow();

        if (Boolean.TRUE.equals(request.getCancelRequest())) {
            String cancelReason = normalizeText(request.getCancelReason());
            if (cancelReason == null) {
                throw new BusinessException("Vui lòng nhập lý do hủy yêu cầu");
            }

            String cancelAction = normalizeText(request.getCancelAction());
            String action = cancelAction == null ? "DELETE" : cancelAction.toUpperCase();

            if ("WAITING_TEAM".equals(action)) {
                entity.setStatus(RescueRequestStatus.PENDING);
                entity.setLocationVerified(true);
                entity.setWaitingForTeam(true);
                entity.setCoordinatorCancelNote(cancelReason);
                entity = rescueRequestRepository.save(entity);

                createTimelineEntry(
                        entity,
                        coordinator,
                        TimelineEventType.CANCEL,
                        RescueRequestStatus.PENDING,
                        RescueRequestStatus.PENDING,
                        "Điều phối đưa yêu cầu về hàng đợi chờ đội cứu hộ rảnh. Lý do: " + cancelReason
                );

                notificationService.notifyUsers(
                        List.of(entity.getCitizen().getId()),
                        "Yêu cầu đang chờ đội cứu hộ rảnh",
                        "Yêu cầu " + entity.getCode() + " đang ở trạng thái chờ đội cứu hộ rảnh. Lý do: " + cancelReason,
                        "CITIZEN_WAITING_TEAM",
                        "RESCUE_REQUEST",
                        entity.getId(),
                        true,
                        null
                );

                return enrichEmergencyActionStatus(mapper.toResponse(entity));
            }

            if (!"DELETE".equals(action)) {
                throw new BusinessException("Hành động hủy không hợp lệ. Chỉ hỗ trợ DELETE hoặc WAITING_TEAM");
            }

            entity.setStatus(RescueRequestStatus.CANCELLED);
            entity.setWaitingForTeam(false);
            entity.setCoordinatorCancelNote(cancelReason);
            entity = rescueRequestRepository.save(entity);

            createTimelineEntry(
                    entity,
                    coordinator,
                    TimelineEventType.CANCEL,
                    RescueRequestStatus.PENDING,
                    RescueRequestStatus.CANCELLED,
                    "Điều phối hủy yêu cầu. Lý do: " + cancelReason
            );

            notificationService.notifyUsers(
                    List.of(entity.getCitizen().getId()),
                    "Yêu cầu cứu hộ đã bị hủy",
                    "Yêu cầu " + entity.getCode() + " đã bị hủy vì lý do: " + cancelReason,
                    "CITIZEN_REQUEST_CANCELLED_BY_COORDINATOR",
                    "RESCUE_REQUEST",
                    entity.getId(),
                    true,
                    null
            );

            return enrichEmergencyActionStatus(mapper.toResponse(entity));
        }

        entity.setWaitingForTeam(false);
        entity.setCoordinatorCancelNote(null);
        entity.setLocationVerified(request.getLocationVerified());
        if (request.getLocationVerified()) {
            entity.setStatus(RescueRequestStatus.VERIFIED);
        }
        entity = rescueRequestRepository.save(entity);

        createTimelineEntry(entity, coordinator, TimelineEventType.VERIFY,
                RescueRequestStatus.PENDING, entity.getStatus(), request.getNote());

        if (Boolean.TRUE.equals(entity.getIsEmergency())
                && entity.getId() != null
                && entity.getStatus() == RescueRequestStatus.VERIFIED) {
            notificationService.markEmergencyConfirmed(coordinatorId, entity.getId());
        }

        return enrichEmergencyActionStatus(mapper.toResponse(entity));
    }

    @Override
    @Transactional
    public RescueRequestResponse prioritizeRescueRequest(Long id, Long coordinatorId, PrioritizeRequest request) {
        RescueRequestEntity entity = rescueRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại"));

        RescuePriority oldPriority = entity.getPriority();
        entity.setPriority(request.getPriority());
        entity = rescueRequestRepository.save(entity);

        UserEntity coordinator = userRepository.findById(coordinatorId).orElseThrow();
        createTimelineEntry(entity, coordinator, TimelineEventType.NOTE,
                null, null, String.format("Thay đổi mức độ ưu tiên từ %s sang %s", oldPriority, request.getPriority()));

        return enrichEmergencyActionStatus(mapper.toResponse(entity));
    }

    @Override
    @Transactional
    public RescueRequestResponse markAsDuplicate(Long id, Long coordinatorId, MarkDuplicateRequest request) {
        RescueRequestEntity entity = rescueRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại"));

        RescueRequestEntity masterRequest = rescueRequestRepository.findById(request.getMasterRequestId())
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ chính không tồn tại"));

        if (masterRequest.getId().equals(id)) {
            throw new BusinessException("Không thể đánh dấu yêu cầu là bản sao của chính nó");
        }

        RescueRequestStatus oldStatus = entity.getStatus();
        entity.setStatus(RescueRequestStatus.DUPLICATE);
        entity.setMasterRequest(masterRequest);
        entity = rescueRequestRepository.save(entity);

        UserEntity coordinator = userRepository.findById(coordinatorId).orElseThrow();
        createTimelineEntry(entity, coordinator, TimelineEventType.MARK_DUPLICATE,
                oldStatus, RescueRequestStatus.DUPLICATE, request.getNote());

        return enrichEmergencyActionStatus(mapper.toResponse(entity));
    }

    @Override
    @Transactional
    public RescueRequestResponse addNote(Long id, Long userId, AddNoteRequest request) {
        RescueRequestEntity entity = rescueRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại"));

        UserEntity user = userRepository.findById(userId).orElseThrow();
        createTimelineEntry(entity, user, TimelineEventType.NOTE, null, null, request.getNote());

        return enrichEmergencyActionStatus(mapper.toResponse(entity));
    }

    @Override
    @Transactional
    public RescueRequestResponse changeStatus(Long id, Long userId, RescueRequestStatus newStatus, String note) {
        RescueRequestEntity entity = rescueRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại"));

        RescueRequestStatus oldStatus = entity.getStatus();
        entity.setStatus(newStatus);
        if (newStatus == RescueRequestStatus.COMPLETED) {
            entity.setRescueResultConfirmationStatus("PENDING");
            entity.setRescueResultConfirmationNote(null);
            entity.setRescueResultConfirmedAt(null);
        }
        entity = rescueRequestRepository.save(entity);

        UserEntity user = userRepository.findById(userId).orElseThrow();
        createTimelineEntry(entity, user, TimelineEventType.STATUS_CHANGE, oldStatus, newStatus, note);

        return enrichEmergencyActionStatus(mapper.toResponse(entity));
    }

    @Override
    @Transactional
    public CitizenRescueConfirmationResponse confirmRescueResult(Long requestId, Long citizenId, Boolean rescued, String reason) {
        RescueRequestEntity original = rescueRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại"));
        original = reconcileStatusFromLatestTaskGroup(original);

        if (!original.getCitizen().getId().equals(citizenId)) {
            throw new BusinessException("Bạn không có quyền xác nhận yêu cầu cứu hộ này");
        }
        if (original.getStatus() != RescueRequestStatus.COMPLETED) {
            throw new BusinessException("Chỉ có thể xác nhận khi yêu cầu đã ở trạng thái COMPLETED");
        }
        if (rescued == null) {
            throw new BusinessException("Thiếu thông tin xác nhận đã được cứu hộ hay chưa");
        }

        UserEntity citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));

        if (Boolean.TRUE.equals(rescued)) {
            original.setRescueResultConfirmationStatus("RESCUED");
            original.setRescueResultConfirmationNote(
                    (reason == null || reason.isBlank()) ? "Citizen xác nhận đã được cứu hộ an toàn." : reason.trim());
            original.setRescueResultConfirmedAt(java.time.LocalDateTime.now());
            rescueRequestRepository.save(original);

            createTimelineEntry(
                    original,
                    citizen,
                    TimelineEventType.NOTE,
                    null,
                    null,
                    "Citizen xác nhận: đã được cứu hộ an toàn."
            );

            return CitizenRescueConfirmationResponse.builder()
                    .rescued(true)
                    .originalRequestId(original.getId())
                    .followUpRequestId(null)
                    .message("Đã xác nhận cứu hộ thành công. Vui lòng đánh giá dịch vụ.")
                    .build();
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Vui lòng nhập lý do chưa được cứu hộ thành công");
        }

        String trimmedReason = reason.trim();
        original.setRescueResultConfirmationStatus("NOT_RESCUED");
        original.setRescueResultConfirmationNote(trimmedReason);
        original.setRescueResultConfirmedAt(java.time.LocalDateTime.now());
        rescueRequestRepository.save(original);
        createTimelineEntry(
                original,
                citizen,
                TimelineEventType.NOTE,
                null,
                null,
                "Citizen phản hồi cứu hộ chưa thành công: " + trimmedReason
        );

        RescueRequestEntity followUp = RescueRequestEntity.builder()
                .code(generateUniqueRescueRequestCode())
                .citizen(original.getCitizen())
                .status(RescueRequestStatus.PENDING)
                .priority(RescuePriority.HIGH)
                .affectedPeopleCount(original.getAffectedPeopleCount())
                .description(buildFollowUpDescriptionForIncompleteRescue(original.getDescription(), trimmedReason))
                .addressText(original.getAddressText())
                .latitude(original.getLatitude())
                .longitude(original.getLongitude())
                .locationDescription(original.getLocationDescription())
                .locationVerified(false)
                .masterRequest(original.getMasterRequest())
                .build();
        followUp = rescueRequestRepository.save(followUp);
        final RescueRequestEntity followUpSaved = followUp;

        List<RescueRequestAttachmentEntity> oldAttachments = attachmentRepository.findByRescueRequestId(original.getId());
        if (oldAttachments != null && !oldAttachments.isEmpty()) {
            List<RescueRequestAttachmentEntity> newAttachments = oldAttachments.stream()
                    .map(att -> RescueRequestAttachmentEntity.builder()
                            .rescueRequest(followUpSaved)
                            .fileUrl(att.getFileUrl())
                            .fileType(att.getFileType())
                            .build())
                    .collect(Collectors.toList());
            attachmentRepository.saveAll(newAttachments);
        }

        createTimelineEntry(
                followUp,
                citizen,
                TimelineEventType.STATUS_CHANGE,
                null,
                RescueRequestStatus.PENDING,
                "Yêu cầu mở lại do rescue chưa thành công thực tế. Mã cũ: " + original.getCode()
        );

        notificationService.notifyRole(
                "COORDINATOR",
                "Yêu cầu cứu hộ mở lại",
                "Citizen báo cứu hộ chưa thành công cho " + original.getCode() + ". Đã tạo yêu cầu mới " + followUp.getCode(),
                "CITIZEN_REOPEN_AFTER_FAILED_RESCUE",
                "RESCUE_REQUEST",
                followUp.getId(),
                true,
                null
        );

        return CitizenRescueConfirmationResponse.builder()
                .rescued(false)
                .originalRequestId(original.getId())
                .followUpRequestId(followUp.getId())
                .message("Đã gửi lại yêu cầu cứu hộ do cứu hộ chưa thành công.")
                .build();
    }

    @Override
    @Transactional
    public void cancelRescueRequest(Long id, Long citizenId) {
        RescueRequestEntity entity = rescueRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại"));

        if (!entity.getCitizen().getId().equals(citizenId)) {
            throw new BusinessException("Bạn không có quyền hủy yêu cầu cứu hộ này");
        }

        if (entity.getStatus() == RescueRequestStatus.COMPLETED ||
                entity.getStatus() == RescueRequestStatus.CANCELLED) {
            throw new BusinessException("Không thể hủy yêu cầu cứu hộ ở trạng thái này");
        }

        RescueRequestStatus oldStatus = entity.getStatus();
        entity.setStatus(RescueRequestStatus.CANCELLED);
        entity = rescueRequestRepository.save(entity);

        UserEntity citizen = userRepository.findById(citizenId).orElseThrow();
        createTimelineEntry(entity, citizen, TimelineEventType.CANCEL,
                oldStatus, RescueRequestStatus.CANCELLED, "Yêu cầu cứu hộ được hủy bởi người tạo");
    }

    @Override
    @Transactional
    public void setCitizenRequestBlockByRequest(Long requestId, Long coordinatorId, boolean blocked, String reason) {
        RescueRequestEntity request = rescueRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại"));

        UserEntity coordinator = userRepository.findById(coordinatorId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        UserEntity citizen = userRepository.findById(request.getCitizen().getId())
                .orElseThrow(() -> new NotFoundException("Citizen không tồn tại"));

        String trimmedReason = normalizeText(reason);
        if (blocked && trimmedReason == null) {
            throw new BusinessException("Vui lòng nhập lý do khi khóa citizen");
        }

        citizen.setRescueRequestBlocked(blocked);
        citizen.setRescueRequestBlockedReason(blocked ? trimmedReason : null);
        userRepository.save(citizen);

        String timelineNote = blocked
                ? "Điều phối khóa quyền gửi yêu cầu của citizen. Lý do: " + trimmedReason
                : "Điều phối mở khóa quyền gửi yêu cầu của citizen.";
        createTimelineEntry(request, coordinator, TimelineEventType.NOTE, null, null, timelineNote);

        if (blocked) {
            notificationService.notifyUsers(
                    List.of(citizen.getId()),
                    "Tài khoản bị khóa gửi yêu cầu cứu hộ",
                    "Bạn tạm thời không thể gửi yêu cầu cứu hộ mới. Lý do: " + trimmedReason,
                    "CITIZEN_REQUEST_BLOCKED",
                    "USER",
                    citizen.getId(),
                    true,
                    null
            );
            return;
        }

        notificationService.notifyUsers(
                List.of(citizen.getId()),
                "Tài khoản đã mở khóa gửi yêu cầu cứu hộ",
                "Bạn đã có thể gửi yêu cầu cứu hộ trở lại.",
                "CITIZEN_REQUEST_UNBLOCKED",
                "USER",
                citizen.getId(),
                false,
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockedCitizenResponse> getBlockedCitizens() {
        return userRepository.findBlockedByRoleCode("CITIZEN").stream()
                .map(u -> BlockedCitizenResponse.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .phone(u.getPhone())
                        .email(u.getEmail())
                        .blockedReason(u.getRescueRequestBlockedReason())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void unblockCitizen(Long citizenId, Long coordinatorId, String reason) {
        UserEntity coordinator = userRepository.findById(coordinatorId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        UserEntity citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new NotFoundException("Citizen không tồn tại"));

        if (citizen.getRole() == null || citizen.getRole().getCode() == null
                || !"CITIZEN".equalsIgnoreCase(citizen.getRole().getCode())) {
            throw new BusinessException("Người dùng này không phải citizen");
        }

        citizen.setRescueRequestBlocked(false);
        citizen.setRescueRequestBlockedReason(null);
        userRepository.save(citizen);

        String note = normalizeText(reason);
        notificationService.notifyUsers(
                List.of(citizen.getId()),
                "Tài khoản đã được gỡ khóa",
                "Bạn đã có thể gửi yêu cầu cứu hộ trở lại."
                        + (note == null ? "" : " Ghi chú: " + note),
                "CITIZEN_REQUEST_UNBLOCKED",
                "USER",
                citizen.getId(),
                false,
                null
        );

        notificationService.notifyUsers(
                List.of(coordinator.getId()),
                "Đã gỡ khóa citizen thành công",
                "Citizen " + citizen.getFullName() + " (ID: " + citizen.getId() + ") đã được gỡ khóa.",
                "COORDINATOR_UNBLOCKED_CITIZEN",
                "USER",
                citizen.getId(),
                false,
                null
        );
    }

    @Override
    @Transactional
    public RescueRequestResponse reopenCancelledRequest(Long requestId, Long citizenId, String reason) {
        RescueRequestEntity entity = rescueRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu hộ không tồn tại"));

        if (!entity.getCitizen().getId().equals(citizenId)) {
            throw new BusinessException("Bạn không có quyền mở lại yêu cầu này");
        }
        if (entity.getStatus() != RescueRequestStatus.CANCELLED) {
            throw new BusinessException("Chỉ có thể gửi lại yêu cầu đang ở trạng thái CANCELLED");
        }

        UserEntity citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        validateCitizenCanCreateRequest(citizen);

        RescueRequestStatus oldStatus = entity.getStatus();
        // Reopen request must be detached from old cancelled task-group links.
        // Otherwise reconcileStatusFromLatestTaskGroup may set it back to CANCELLED.
        taskGroupRequestRepository.deleteByRescueRequestId(entity.getId());
        entity.setStatus(RescueRequestStatus.PENDING);
        entity.setLocationVerified(false);
        entity.setWaitingForTeam(false);
        entity.setCoordinatorCancelNote(null);
        String reopenReason = normalizeText(reason);
        entity.setDescription(buildReopenAfterCancelledDescription(entity.getDescription(), reopenReason));
        entity = rescueRequestRepository.save(entity);

        createTimelineEntry(
                entity,
                citizen,
                TimelineEventType.STATUS_CHANGE,
                oldStatus,
                RescueRequestStatus.PENDING,
                reopenReason == null ? "Citizen gửi lại yêu cầu cứu hộ." : "Citizen gửi lại yêu cầu: " + reopenReason
        );

        notificationService.notifyRole(
                "COORDINATOR",
                "Citizen gửi lại yêu cầu đã hủy",
                "Citizen vừa gửi lại yêu cầu " + entity.getCode()
                        + (reopenReason == null ? "" : ". Lý do: " + reopenReason),
                "CITIZEN_REOPEN_CANCELLED_REQUEST",
                "RESCUE_REQUEST",
                entity.getId(),
                true,
                null
        );

        return enrichEmergencyActionStatus(mapper.toResponse(entity));
    }

    private void createTimelineEntry(
            RescueRequestEntity rescueRequest,
            UserEntity actor,
            TimelineEventType eventType,
            RescueRequestStatus fromStatus,
            RescueRequestStatus toStatus,
            String note) {

        RescueRequestTimelineEntity timeline = RescueRequestTimelineEntity.builder()
                .rescueRequest(rescueRequest)
                .actor(actor)
                .eventType(eventType)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .note(note)
                .build();

        timelineRepository.save(timeline);
    }

    private String generateUniqueRescueRequestCode() {
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

    private String buildFollowUpDescriptionForIncompleteRescue(String oldDescription, String reason) {
        String oldText = oldDescription == null ? "" : oldDescription.trim();
        String retry = "Chưa được cứu hộ nhưng đội cứu hộ ấn hoàn thành. Lý do công dân phản hồi: " + reason;
        if (oldText.isBlank()) {
            return retry;
        }
        return oldText + "\n\n---\n" + retry;
    }

    private String buildReopenAfterCancelledDescription(String oldDescription, String reason) {
        String oldText = oldDescription == null ? "" : oldDescription.trim();
        String marker = "Yêu cầu bị huỷ.";
        if (reason != null && !reason.isBlank()) {
            marker += " Lý do gửi lại: " + reason;
        }

        if (oldText.isBlank()) {
            return marker;
        }
        return oldText + "\n\n---\n" + marker;
    }

    private RescueRequestEntity reconcileStatusFromLatestTaskGroup(RescueRequestEntity entity) {
        if (entity == null || entity.getId() == null) {
            return entity;
        }
        List<TaskGroupRequestEntity> links = taskGroupRequestRepository.findByRescueRequestId(entity.getId());
        if (links == null || links.isEmpty()) {
            return entity;
        }
        TaskGroupStatus latestGroupStatus = links.stream()
                .filter(link -> link.getTaskGroup() != null && link.getTaskGroup().getId() != null)
                .max(java.util.Comparator.comparing(link -> link.getTaskGroup().getId()))
                .map(link -> link.getTaskGroup().getStatus())
                .orElse(null);
        RescueRequestStatus mapped = mapTaskGroupStatusToRescueRequestStatus(latestGroupStatus);
        if (mapped == null || mapped == entity.getStatus()) {
            return entity;
        }
        entity.setStatus(mapped);
        if (mapped == RescueRequestStatus.COMPLETED) {
            entity.setRescueResultConfirmationStatus("PENDING");
            entity.setRescueResultConfirmationNote(null);
            entity.setRescueResultConfirmedAt(null);
        }
        return rescueRequestRepository.save(entity);
    }

    private void reconcileStatusFromLatestTaskGroup(List<RescueRequestEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        entities.forEach(this::reconcileStatusFromLatestTaskGroup);
    }

    private RescueRequestStatus mapTaskGroupStatusToRescueRequestStatus(TaskGroupStatus taskStatus) {
        if (taskStatus == null) {
            return null;
        }
        return switch (taskStatus) {
            case ASSIGNED -> RescueRequestStatus.ASSIGNED;
            case IN_PROGRESS -> RescueRequestStatus.IN_PROGRESS;
            case DONE -> RescueRequestStatus.COMPLETED;
            case CANCELLED -> RescueRequestStatus.CANCELLED;
            default -> null;
        };
    }

    private void saveAttachments(RescueRequestEntity rescueRequest, List<RescueRequestUpdateRequest.AttachmentRequest> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        List<RescueRequestAttachmentEntity> entities = attachments.stream()
                .map(att -> RescueRequestAttachmentEntity.builder()
                        .rescueRequest(rescueRequest)
                        .fileUrl(att.getFileUrl())
                        .fileType(att.getFileType())
                        .build())
                .collect(Collectors.toList());
        attachmentRepository.saveAll(entities);
    }

    private Page<RescueRequestResponse> enrichEmergencyActionStatusPage(Page<RescueRequestResponse> page) {
        enrichEmergencyActionStatus(page.getContent());
        return page;
    }

    private RescueRequestResponse enrichEmergencyActionStatus(RescueRequestResponse response) {
        if (response == null) {
            return null;
        }
        enrichEmergencyActionStatus(List.of(response));
        return response;
    }

    private void enrichEmergencyActionStatus(List<RescueRequestResponse> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (RescueRequestResponse item : items) {
            item.setWaitingCitizenRescueConfirmation(
                    item.getStatus() == RescueRequestStatus.COMPLETED
                            && (item.getRescueResultConfirmationStatus() == null
                            || item.getRescueResultConfirmationStatus().isBlank()
                            || "PENDING".equalsIgnoreCase(item.getRescueResultConfirmationStatus()))
            );
        }

        List<Long> queueIds = items.stream()
                .filter(r -> Boolean.TRUE.equals(r.getEmergency()))
                .map(RescueRequestResponse::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (queueIds.isEmpty()) {
            return;
        }

        List<NotificationEntity> emergencyNotifs = notificationRepository.findByEventCodeAndQueueRequestIdIn("RESCUER_EMERGENCY", queueIds);
        if (emergencyNotifs.isEmpty()) {
            return;
        }

        Map<Long, String> statusByQueueId = emergencyNotifs.stream()
                .filter(n -> n.getQueueRequestId() != null)
                .collect(Collectors.toMap(
                        NotificationEntity::getQueueRequestId,
                        NotificationEntity::getActionStatus,
                        this::pickPreferredEmergencyStatus
                ));

        for (RescueRequestResponse item : items) {
            if (!Boolean.TRUE.equals(item.getEmergency()) || item.getId() == null) {
                continue;
            }
            item.setEmergencyActionStatus(statusByQueueId.get(item.getId()));
        }
    }

    private String pickPreferredEmergencyStatus(String current, String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return current;
        }
        if (current == null || current.isBlank()) {
            return incoming;
        }
        int currentPriority = emergencyStatusPriority(current);
        int incomingPriority = emergencyStatusPriority(incoming);
        return incomingPriority >= currentPriority ? incoming : current;
    }

    private int emergencyStatusPriority(String statusRaw) {
        String status = String.valueOf(statusRaw).toUpperCase();
        return switch (status) {
            case "REASSIGNED" -> 5;
            case "CONFIRMED" -> 4;
            case "WAITING_OVERLOAD" -> 3;
            case "QUEUED" -> 2;
            case "VIEWED" -> 1;
            default -> 0;
        };
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateCitizenCanCreateRequest(UserEntity citizen) {
        if (Boolean.TRUE.equals(citizen.getRescueRequestBlocked())) {
            String reason = normalizeText(citizen.getRescueRequestBlockedReason());
            if (reason == null) {
                throw new BusinessException("Tài khoản đang bị khóa gửi yêu cầu cứu hộ");
            }
            throw new BusinessException("Tài khoản đang bị khóa gửi yêu cầu cứu hộ. Lý do: " + reason);
        }
    }
}
