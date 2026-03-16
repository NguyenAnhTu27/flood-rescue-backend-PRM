package com.floodrescue.module.relief.dto.response;

import com.floodrescue.shared.enums.InventoryDocumentStatus;
import com.floodrescue.shared.enums.ReliefDeliveryStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ReliefRequestResponse {

    private Long id;
    private String code;
    private InventoryDocumentStatus status;
    private String targetArea;
    private Long createdById;
    private String createdByName;
    private String createdByPhone;
    private Long rescueRequestId;
    private String citizenAddressText;
    private Double citizenLatitude;
    private Double citizenLongitude;
    private String citizenLocationDescription;
    private String note;
    private ReliefDeliveryStatus deliveryStatus;
    private Long assignedTeamId;
    private Long approvedById;
    private Long assignedIssueId;
    private String deliveryNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<LineItem> lines;

    @Getter
    @Setter
    @Builder
    public static class LineItem {
        private Long id;
        private Integer itemCategoryId;
        private String itemCode;
        private String itemName;
        private BigDecimal qty;
        private String unit;
    }
}
