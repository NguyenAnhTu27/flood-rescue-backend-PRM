package com.floodrescue.module.relief.service;

import com.floodrescue.module.inventory.entity.ItemCategoryEntity;
import com.floodrescue.module.inventory.dto.request.InventoryIssueCreateRequest;
import com.floodrescue.module.inventory.dto.request.InventoryIssueLineRequest;
import com.floodrescue.module.inventory.dto.response.InventoryIssueResponse;
import com.floodrescue.module.inventory.entity.InventoryIssueEntity;
import com.floodrescue.module.inventory.repository.ItemCategoryRepository;
import com.floodrescue.module.inventory.repository.IssueRepository;
import com.floodrescue.module.inventory.service.IssueService;
import com.floodrescue.module.relief.dto.request.ReliefRequestCreateRequest;
import com.floodrescue.module.relief.dto.response.ReliefRequestResponse;
import com.floodrescue.module.relief.entity.ReliefRequestEntity;
import com.floodrescue.module.relief.entity.ReliefRequestLineEntity;
import com.floodrescue.module.notification.service.NotificationService;
import com.floodrescue.module.relief.reponsitory.ReliefRequestLineRepository;
import com.floodrescue.module.relief.reponsitory.ReliefRequestRepository;
import com.floodrescue.module.rescue.entity.RescueRequestEntity;
import com.floodrescue.module.rescue.repository.RescueRequestRepository;
import com.floodrescue.shared.enums.InventoryDocumentStatus;
import com.floodrescue.shared.enums.ReliefDeliveryStatus;
import com.floodrescue.shared.exception.BusinessException;
import com.floodrescue.shared.exception.NotFoundException;
import com.floodrescue.shared.util.CodeGenerator;
import com.floodrescue.module.user.entity.UserEntity;
import com.floodrescue.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReliefRequestService {

    private final ReliefRequestRepository reliefRequestRepository;
    private final ReliefRequestLineRepository reliefRequestLineRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final RescueRequestRepository rescueRequestRepository;
    private final IssueService issueService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final IssueRepository issueRepository;

    @Transactional
    public ReliefRequestResponse createReliefRequest(Long userId, ReliefRequestCreateRequest request) {
        if (userId == null) {
            throw new BusinessException("Không xác định được người tạo yêu cầu cứu trợ");
        }

        String code = CodeGenerator.generateInventoryReceiptCode(); // có thể tách hàm generate riêng nếu cần

        RescueRequestEntity rescueRequest = null;
        if (request.getRescueRequestId() != null) {
            rescueRequest = rescueRequestRepository.findById(request.getRescueRequestId())
                    .orElseThrow(() -> new NotFoundException("Yêu cầu cứu nạn không tồn tại: " + request.getRescueRequestId()));
        }

        ReliefRequestEntity relief = ReliefRequestEntity.builder()
                .code(code)
                .createdById(userId)
                .status(InventoryDocumentStatus.DRAFT)
                .deliveryStatus(ReliefDeliveryStatus.REQUESTED)
                .targetArea(request.getTargetArea().trim())
                .addressText(request.getAddressText())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationDescription(request.getLocationDescription())
                .rescueRequest(rescueRequest)
                .note(request.getNote())
                .build();

        ReliefRequestEntity saved = reliefRequestRepository.save(relief);

        List<ReliefRequestLineEntity> lines = List.of();
        if (request.getLines() != null && !request.getLines().isEmpty()) {
            lines = request.getLines().stream()
                    .map(lineReq -> {
                        ItemCategoryEntity category = itemCategoryRepository.findById(lineReq.getItemCategoryId())
                                .orElseThrow(() -> new NotFoundException("Loại hàng không tồn tại: " + lineReq.getItemCategoryId()));
                        return ReliefRequestLineEntity.builder()
                                .reliefRequest(saved)
                                .itemCategory(category)
                                .qty(BigDecimal.valueOf(lineReq.getQty()))
                                .unit(lineReq.getUnit())
                                .build();
                    })
                    .collect(Collectors.toList());
            reliefRequestLineRepository.saveAll(lines);
        }

        return toResponse(saved, lines);
    }

    @Transactional
    public ReliefRequestResponse approveAndDispatch(Long reliefRequestId, Long managerId, Long assignedTeamId, String note) {
        ReliefRequestEntity relief = reliefRequestRepository.findById(reliefRequestId)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu trợ không tồn tại"));
        if (relief.getStatus() != InventoryDocumentStatus.DRAFT) {
            throw new BusinessException("Chỉ có thể duyệt yêu cầu cứu trợ ở trạng thái DRAFT");
        }

        List<ReliefRequestLineEntity> lines = reliefRequestLineRepository.findByReliefRequest(relief);
        if (lines.isEmpty()) {
            throw new BusinessException("Yêu cầu cứu trợ không có mặt hàng");
        }

        InventoryIssueCreateRequest issueRequest = new InventoryIssueCreateRequest();
        issueRequest.setReliefRequestId(relief.getId());
        issueRequest.setAssignedTeamId(assignedTeamId);
        issueRequest.setNote(note == null || note.isBlank() ? "Phiếu xuất tự động từ yêu cầu cứu trợ " + relief.getCode() : note.trim());
        issueRequest.setLines(lines.stream().map(l -> {
            InventoryIssueLineRequest line = new InventoryIssueLineRequest();
            line.setItemCategoryId(l.getItemCategory().getId());
            line.setQty(l.getQty().doubleValue());
            line.setUnit(l.getUnit());
            return line;
        }).collect(Collectors.toList()));

        InventoryIssueResponse createdIssue = issueService.createIssue(managerId, issueRequest);

        relief.setStatus(InventoryDocumentStatus.APPROVED);
        relief.setApprovedById(managerId);
        relief.setAssignedTeamId(assignedTeamId);
        relief.setAssignedIssueId(createdIssue.getId());
        relief.setDeliveryStatus(ReliefDeliveryStatus.MANAGER_APPROVED);
        relief.setDeliveryNote(note);
        relief = reliefRequestRepository.save(relief);

        notifyCitizen(relief, "Yêu cầu cứu trợ đã được duyệt",
                "Yêu cầu " + relief.getCode() + " đã được quản lý duyệt và chuyển cho đội cứu hộ.");
        notifyRescuerTeam(assignedTeamId, relief,
                "Bạn có yêu cầu cứu trợ mới",
                "Yêu cầu " + relief.getCode() + " đã được giao cho đội của bạn.");

        return toResponse(relief, lines);
    }

    @Transactional
    public ReliefRequestResponse rejectRequest(Long reliefRequestId, String reason) {
        ReliefRequestEntity relief = reliefRequestRepository.findById(reliefRequestId)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu trợ không tồn tại"));

        // Cho phép manager/admin từ chối ở mọi trạng thái theo yêu cầu vận hành.
        // Action này luôn đưa yêu cầu về trạng thái bị hủy/từ chối.
        relief.setStatus(InventoryDocumentStatus.CANCELLED);
        relief.setDeliveryStatus(ReliefDeliveryStatus.REJECTED);
        relief.setDeliveryNote(reason);
        relief = reliefRequestRepository.save(relief);
        List<ReliefRequestLineEntity> lines = reliefRequestLineRepository.findByReliefRequest(relief);
        notifyCitizen(relief, "Yêu cầu cứu trợ bị từ chối",
                "Yêu cầu " + relief.getCode() + " đã bị từ chối. " + (reason == null ? "" : reason));
        return toResponse(relief, lines);
    }

    @Transactional
    public ReliefRequestResponse updateCitizenReliefRequest(Long reliefRequestId, Long citizenId, ReliefRequestCreateRequest request) {
        ReliefRequestEntity relief = reliefRequestRepository.findById(reliefRequestId)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu trợ không tồn tại"));

        if (!relief.getCreatedById().equals(citizenId)) {
            throw new BusinessException("Bạn không có quyền cập nhật yêu cầu cứu trợ này");
        }
        if (relief.getStatus() != InventoryDocumentStatus.DRAFT) {
            throw new BusinessException("Chỉ có thể cập nhật yêu cầu đang ở trạng thái DRAFT");
        }

        RescueRequestEntity rescueRequest = null;
        if (request.getRescueRequestId() != null) {
            rescueRequest = rescueRequestRepository.findById(request.getRescueRequestId())
                    .orElseThrow(() -> new NotFoundException("Yêu cầu cứu nạn không tồn tại: " + request.getRescueRequestId()));
        }

        relief.setTargetArea(request.getTargetArea().trim());
        relief.setAddressText(request.getAddressText());
        relief.setLatitude(request.getLatitude());
        relief.setLongitude(request.getLongitude());
        relief.setLocationDescription(request.getLocationDescription());
        relief.setRescueRequest(rescueRequest);
        relief.setNote(request.getNote());
        final ReliefRequestEntity savedRelief = reliefRequestRepository.save(relief);

        if (request.getLines() != null) {
            List<ReliefRequestLineEntity> oldLines = reliefRequestLineRepository.findByReliefRequest(savedRelief);
            if (!oldLines.isEmpty()) {
                reliefRequestLineRepository.deleteAll(oldLines);
            }

            if (!request.getLines().isEmpty()) {
                List<ReliefRequestLineEntity> newLines = request.getLines().stream()
                        .map(lineReq -> {
                            ItemCategoryEntity category = itemCategoryRepository.findById(lineReq.getItemCategoryId())
                                    .orElseThrow(() -> new NotFoundException("Loại hàng không tồn tại: " + lineReq.getItemCategoryId()));
                            return ReliefRequestLineEntity.builder()
                                    .reliefRequest(savedRelief)
                                    .itemCategory(category)
                                    .qty(BigDecimal.valueOf(lineReq.getQty()))
                                    .unit(lineReq.getUnit())
                                    .build();
                        })
                        .collect(Collectors.toList());
                reliefRequestLineRepository.saveAll(newLines);
            }
        }

        List<ReliefRequestLineEntity> lines = reliefRequestLineRepository.findByReliefRequest(savedRelief);
        return toResponse(savedRelief, lines);
    }

    @Transactional
    public void cancelCitizenReliefRequest(Long reliefRequestId, Long citizenId, String reason) {
        ReliefRequestEntity relief = reliefRequestRepository.findById(reliefRequestId)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu trợ không tồn tại"));

        if (!relief.getCreatedById().equals(citizenId)) {
            throw new BusinessException("Bạn không có quyền hủy yêu cầu cứu trợ này");
        }
        if (relief.getStatus() != InventoryDocumentStatus.DRAFT) {
            throw new BusinessException("Chỉ có thể hủy yêu cầu đang ở trạng thái DRAFT");
        }

        relief.setStatus(InventoryDocumentStatus.CANCELLED);
        relief.setDeliveryStatus(ReliefDeliveryStatus.REJECTED);
        relief.setDeliveryNote(reason == null || reason.isBlank() ? "Citizen tự hủy yêu cầu cứu trợ." : reason.trim());
        reliefRequestRepository.save(relief);
    }

    @Transactional(readOnly = true)
    public Page<ReliefRequestResponse> listMyReliefRequests(Long citizenId, Pageable pageable) {
        return reliefRequestRepository.findByCreatedByIdOrderByCreatedAtDesc(citizenId, pageable)
                .map(r -> toResponse(r, reliefRequestLineRepository.findByReliefRequest(r)));
    }

    @Transactional(readOnly = true)
    public Page<ReliefRequestResponse> listRescuerAssignedReliefRequests(Long teamId, Pageable pageable) {
        return reliefRequestRepository.findByAssignedTeamIdOrderByUpdatedAtDesc(teamId, pageable)
                .map(r -> toResponse(r, reliefRequestLineRepository.findByReliefRequest(r)));
    }

    @Transactional
    public ReliefRequestResponse updateRescuerDeliveryStatus(Long rescuerUserId, Long reliefRequestId, ReliefDeliveryStatus status, String note) {
        if (status == null || !Arrays.asList(
                ReliefDeliveryStatus.RESCUER_RECEIVED,
                ReliefDeliveryStatus.ARRIVED_WAREHOUSE,
                ReliefDeliveryStatus.ARRIVED_RELIEF_POINT,
                ReliefDeliveryStatus.COMPLETED,
                ReliefDeliveryStatus.RETURNED_TO_WAREHOUSE
        ).contains(status)) {
            throw new BusinessException("Trạng thái cập nhật không hợp lệ cho rescuer");
        }

        UserEntity rescuer = userRepository.findById(rescuerUserId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        if (rescuer.getTeamId() == null) {
            throw new BusinessException("Tài khoản rescuer chưa được gán đội");
        }

        ReliefRequestEntity relief = reliefRequestRepository.findById(reliefRequestId)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu trợ không tồn tại"));
        if (relief.getAssignedTeamId() == null || !relief.getAssignedTeamId().equals(rescuer.getTeamId())) {
            throw new BusinessException("Yêu cầu cứu trợ không thuộc đội của bạn");
        }
        if (relief.getStatus() == InventoryDocumentStatus.CANCELLED) {
            throw new BusinessException("Yêu cầu cứu trợ đã bị hủy");
        }

        relief.setDeliveryStatus(status);
        if (note != null && !note.isBlank()) {
            relief.setDeliveryNote(note.trim());
        }
        if (status == ReliefDeliveryStatus.COMPLETED) {
            relief.setStatus(InventoryDocumentStatus.DONE);
        }

        InventoryIssueEntity issue = null;
        if (relief.getAssignedIssueId() != null) {
            issue = issueRepository.findById(relief.getAssignedIssueId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu xuất đã gán"));
        } else {
            issue = issueRepository.findFirstByReliefRequestIdOrderByIdDesc(relief.getId()).orElse(null);
            if (issue != null) {
                relief.setAssignedIssueId(issue.getId());
            }
        }

        if (issue != null) {
            if (status == ReliefDeliveryStatus.ARRIVED_RELIEF_POINT && issue.getStatus() == InventoryDocumentStatus.DRAFT) {
                issueService.markIssueTemporaryDeduction(issue.getId(), rescuerUserId);
            } else if (status == ReliefDeliveryStatus.COMPLETED && issue.getStatus() == InventoryDocumentStatus.APPROVED) {
                issueService.finalizeIssueDeduction(issue.getId(), rescuerUserId);
            } else if (status == ReliefDeliveryStatus.RETURNED_TO_WAREHOUSE && issue.getStatus() == InventoryDocumentStatus.APPROVED) {
                issueService.returnIssueToWarehouse(issue.getId(), rescuerUserId, note);
                relief.setStatus(InventoryDocumentStatus.CANCELLED);
            }
        }

        relief = reliefRequestRepository.save(relief);

        List<ReliefRequestLineEntity> lines = reliefRequestLineRepository.findByReliefRequest(relief);
        notifyCitizen(relief, "Yêu cầu cứu trợ cập nhật trạng thái",
                "Yêu cầu " + relief.getCode() + " đang ở trạng thái: " + status.name());
        return toResponse(relief, lines);
    }

    @Transactional(readOnly = true)
    public ReliefRequestResponse getReliefRequest(Long id) {
        ReliefRequestEntity relief = reliefRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Yêu cầu cứu trợ không tồn tại"));
        List<ReliefRequestLineEntity> lines = reliefRequestLineRepository.findByReliefRequest(relief);
        return toResponse(relief, lines);
    }

    @Transactional(readOnly = true)
    public Page<ReliefRequestResponse> listReliefRequests(InventoryDocumentStatus status, Pageable pageable) {
        Page<ReliefRequestEntity> page;
        if (status != null) {
            page = reliefRequestRepository.findByStatus(status, pageable);
        } else {
            page = reliefRequestRepository.findAll(pageable);
        }
        return page.map(r -> toResponse(r, reliefRequestLineRepository.findByReliefRequest(r)));
    }

    private ReliefRequestResponse toResponse(ReliefRequestEntity relief, List<ReliefRequestLineEntity> lines) {
        Long rescueRequestId = relief.getRescueRequest() != null ? relief.getRescueRequest().getId() : null;
        UserEntity createdBy = userRepository.findById(relief.getCreatedById()).orElse(null);
        RescueRequestEntity rescue = relief.getRescueRequest();
        List<ReliefRequestResponse.LineItem> responseLines = new ArrayList<>();
        if (lines != null && !lines.isEmpty()) {
            responseLines = lines.stream().map(l -> ReliefRequestResponse.LineItem.builder()
                    .id(l.getId())
                    .itemCategoryId(l.getItemCategory().getId())
                    .itemCode(l.getItemCategory().getCode())
                    .itemName(l.getItemCategory().getName())
                    .qty(l.getQty())
                    .unit(l.getUnit())
                    .build()).collect(Collectors.toList());
        } else if (relief.getAssignedIssueId() != null) {
            // Fallback cho case dữ liệu hàng chỉ nằm ở phiếu xuất kho:
            // truy theo assignedIssueId để trả đúng danh sách hàng đội cần giao.
            try {
                InventoryIssueResponse issue = issueService.getIssue(relief.getAssignedIssueId());
                if (issue != null && issue.getLines() != null) {
                    responseLines = issue.getLines().stream().map(l -> ReliefRequestResponse.LineItem.builder()
                            .id(l.getId())
                            .itemCategoryId(l.getItemCategoryId())
                            .itemCode(l.getItemCode())
                            .itemName(l.getItemName())
                            .qty(l.getQty())
                            .unit(l.getUnit())
                            .build()).collect(Collectors.toList());
                }
            } catch (Exception ignored) {
                // Keep empty lines when issue is unavailable.
            }
        }

        return ReliefRequestResponse.builder()
                .id(relief.getId())
                .code(relief.getCode())
                .status(relief.getStatus())
                .targetArea(relief.getTargetArea())
                .createdById(relief.getCreatedById())
                .createdByName(createdBy != null ? createdBy.getFullName() : null)
                .createdByPhone(createdBy != null ? createdBy.getPhone() : null)
                .rescueRequestId(rescueRequestId)
                .citizenAddressText(
                        relief.getAddressText() != null ? relief.getAddressText()
                                : (rescue != null ? rescue.getAddressText() : null)
                )
                .citizenLatitude(
                        relief.getLatitude() != null ? relief.getLatitude()
                                : (rescue != null ? rescue.getLatitude() : null)
                )
                .citizenLongitude(
                        relief.getLongitude() != null ? relief.getLongitude()
                                : (rescue != null ? rescue.getLongitude() : null)
                )
                .citizenLocationDescription(
                        relief.getLocationDescription() != null ? relief.getLocationDescription()
                                : (rescue != null ? rescue.getLocationDescription() : null)
                )
                .note(relief.getNote())
                .deliveryStatus(relief.getDeliveryStatus())
                .assignedTeamId(relief.getAssignedTeamId())
                .approvedById(relief.getApprovedById())
                .assignedIssueId(relief.getAssignedIssueId())
                .deliveryNote(relief.getDeliveryNote())
                .createdAt(relief.getCreatedAt())
                .updatedAt(relief.getUpdatedAt())
                .lines(responseLines)
                .build();
    }

    private void notifyCitizen(ReliefRequestEntity relief, String title, String content) {
        notificationService.notifyUsers(
                List.of(relief.getCreatedById()),
                title,
                content,
                "RELIEF_REQUEST_STATUS",
                "RELIEF_REQUEST",
                relief.getId(),
                false,
                relief.getAssignedTeamId()
        );
    }

    private void notifyRescuerTeam(Long teamId, ReliefRequestEntity relief, String title, String content) {
        if (teamId == null) return;
        List<Long> rescuerIds = userRepository.findAllByTeamIdAndRoleCode(teamId, "RESCUER")
                .stream()
                .map(UserEntity::getId)
                .toList();
        notificationService.notifyUsers(
                rescuerIds,
                title,
                content,
                "RELIEF_REQUEST_ASSIGNED",
                "RELIEF_REQUEST",
                relief.getId(),
                true,
                teamId
        );
    }
}
