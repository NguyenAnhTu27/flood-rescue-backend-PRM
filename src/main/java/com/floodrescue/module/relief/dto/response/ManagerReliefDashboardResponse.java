package com.floodrescue.module.relief.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ManagerReliefDashboardResponse {

    private List<StatItem> overview;
    private List<TransactionItem> recentTransactions;
    private List<InventorySummaryItem> inventorySummary;
    private List<InventoryItem> inventoryItems;

    @Getter
    @Setter
    @Builder
    public static class StatItem {
        private String id;
        private String label;
        private String value;
        private String unit;
        private String sub;
        private String color;
        private Boolean highlighted;
    }

    @Getter
    @Setter
    @Builder
    public static class TransactionItem {
        private String id;
        private String code;
        private String type;
        private String typeLabel;
        private String typeColor;
        private String destination;
        private String status;
        private String statusLabel;
        private String statusColor;
        private String time;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @Builder
    public static class InventorySummaryItem {
        private String id;
        private String label;
        private String value;
        private String color;
    }

    @Getter
    @Setter
    @Builder
    public static class InventoryItem {
        private String code;
        private String name;
        private String categoryName;
        private String unit;
        private BigDecimal qty;
        private String status;
        private String statusLabel;
        private String statusColor;
    }
}

