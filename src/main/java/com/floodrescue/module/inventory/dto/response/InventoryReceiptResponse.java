package com.floodrescue.module.inventory.dto.response;

import com.floodrescue.shared.enums.InventoryDocumentStatus;
import com.floodrescue.shared.enums.StockSourceType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class InventoryReceiptResponse {

    private Long id;
    private String code;
    private StockSourceType sourceType;
    private InventoryDocumentStatus status;
    private Long createdById;
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