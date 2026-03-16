package com.floodrescue.module.relief.dto.response;

import com.floodrescue.shared.enums.DistributionPriority;
import com.floodrescue.shared.enums.InventoryDocumentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class DistributionVoucherResponse {

    private Long id;
    private String code;
    private InventoryDocumentStatus status;
    private Long createdById;

    private Long reliefRequestId;
    private String reliefRequestCode;

    private Long issueId;
    private String issueCode;

    private Long teamId;
    private String teamCode;
    private String teamName;

    private Long assetId;
    private String assetCode;
    private String assetName;

    private String receiverName;
    private String receiverPhone;
    private String deliveryAddress;
    private LocalDateTime eta;
    private DistributionPriority priority;
    private String note;
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
