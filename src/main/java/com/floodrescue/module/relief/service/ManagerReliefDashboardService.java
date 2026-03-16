package com.floodrescue.module.relief.service;

import com.floodrescue.module.inventory.entity.InventoryIssueEntity;
import com.floodrescue.module.inventory.entity.InventoryReceiptEntity;
import com.floodrescue.module.inventory.entity.ItemCategoryEntity;
import com.floodrescue.module.inventory.entity.StockBalanceEntity;
import com.floodrescue.module.inventory.repository.IssueRepository;
import com.floodrescue.module.inventory.repository.ReceipRepository;
import com.floodrescue.module.inventory.repository.ItemCategoryRepository;
import com.floodrescue.module.inventory.repository.StockBalanceRepository;
import com.floodrescue.module.relief.dto.response.ManagerReliefDashboardResponse;
import com.floodrescue.shared.enums.InventoryDocumentStatus;
import com.floodrescue.shared.enums.StockSourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerReliefDashboardService {

    private final ItemCategoryRepository itemCategoryRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final ReceipRepository receipRepository;
    private final IssueRepository issueRepository;

    @Transactional(readOnly = true)
    public ManagerReliefDashboardResponse getDashboard() {
        List<ManagerReliefDashboardResponse.StatItem> overview = buildOverview();
        List<ManagerReliefDashboardResponse.TransactionItem> transactions = buildTransactions();
        List<ManagerReliefDashboardResponse.InventorySummaryItem> invSummary = buildInventorySummary();
        List<ManagerReliefDashboardResponse.InventoryItem> invItems = buildInventoryItems();

        return ManagerReliefDashboardResponse.builder()
                .overview(overview)
                .recentTransactions(transactions)
                .inventorySummary(invSummary)
                .inventoryItems(invItems)
                .build();
    }

    private List<ManagerReliefDashboardResponse.StatItem> buildOverview() {
        long totalItems = itemCategoryRepository.count();
        long totalReceipts = receipRepository.count();
        long totalIssues = issueRepository.count();

        List<ManagerReliefDashboardResponse.StatItem> stats = new ArrayList<>();
        stats.add(ManagerReliefDashboardResponse.StatItem.builder()
                .id("items")
                .label("Loại hàng cứu trợ")
                .value(String.valueOf(totalItems))
                .unit("Loại")
                .sub(null)
                .color("slate")
                .highlighted(false)
                .build());

        stats.add(ManagerReliefDashboardResponse.StatItem.builder()
                .id("imports")
                .label("Số phiếu nhập")
                .value(String.valueOf(totalReceipts))
                .unit("Phiếu")
                .sub(null)
                .color("slate")
                .highlighted(false)
                .build());

        stats.add(ManagerReliefDashboardResponse.StatItem.builder()
                .id("exports")
                .label("Số phiếu xuất")
                .value(String.valueOf(totalIssues))
                .unit("Phiếu")
                .sub(null)
                .color("slate")
                .highlighted(false)
                .build());

        return stats;
    }

    private List<ManagerReliefDashboardResponse.TransactionItem> buildTransactions() {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm dd/MM");

        var latestReceipts = receipRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        var latestIssues = issueRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        List<ManagerReliefDashboardResponse.TransactionItem> list = new ArrayList<>();

        for (InventoryReceiptEntity r : latestReceipts) {
            list.add(ManagerReliefDashboardResponse.TransactionItem.builder()
                    .id(r.getCode())
                    .code(r.getCode())
                    .type("Nhập kho")
                    .typeLabel("Nhập kho")
                    .typeColor("blue")
                    .destination("Kho trung tâm") // có thể thay bằng tên kho nếu có
                    .statusLabel(r.getStatus().name())
                    .status(r.getStatus().name())
                    .statusColor(r.getStatus() == InventoryDocumentStatus.DONE ? "green" : "blue")
                    .time(r.getCreatedAt() != null ? r.getCreatedAt().format(timeFmt) : "")
                    .createdAt(r.getCreatedAt())
                    .build());
        }

        for (InventoryIssueEntity i : latestIssues) {
            list.add(ManagerReliefDashboardResponse.TransactionItem.builder()
                    .id(i.getCode())
                    .code(i.getCode())
                    .type("Xuất kho")
                    .typeLabel("Xuất kho")
                    .typeColor("orange")
                    .destination("Điểm phát cứu trợ") // placeholder
                    .statusLabel(i.getStatus().name())
                    .status(i.getStatus().name())
                    .statusColor(i.getStatus() == InventoryDocumentStatus.DONE ? "green" : "blue")
                    .time(i.getCreatedAt() != null ? i.getCreatedAt().format(timeFmt) : "")
                    .createdAt(i.getCreatedAt())
                    .build());
        }

        // sắp xếp chung theo createdAt desc
        return list.stream()
                .sorted((a, b) -> {
                    LocalDateTime ta = a.getCreatedAt();
                    LocalDateTime tb = b.getCreatedAt();
                    if (ta == null || tb == null) return 0;
                    return tb.compareTo(ta);
                })
                .limit(8)
                .collect(Collectors.toList());
    }

    private List<ManagerReliefDashboardResponse.InventorySummaryItem> buildInventorySummary() {
        List<StockBalanceEntity> balances = stockBalanceRepository.findAll();

        BigDecimal totalQty = balances.stream()
                .map(StockBalanceEntity::getQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();

        BigDecimal todayImports = receipRepository.findByStatus(InventoryDocumentStatus.DONE,
                        PageRequest.of(0, Integer.MAX_VALUE))
                .stream()
                .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(startOfDay))
                .map(r -> BigDecimal.ZERO) // có thể cộng theo lines nếu cần chi tiết
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal todayIssues = issueRepository.findByStatus(InventoryDocumentStatus.DONE,
                        PageRequest.of(0, Integer.MAX_VALUE))
                .stream()
                .filter(i -> i.getCreatedAt() != null && !i.getCreatedAt().isBefore(startOfDay))
                .map(i -> BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ManagerReliefDashboardResponse.InventorySummaryItem> list = new ArrayList<>();
        list.add(ManagerReliefDashboardResponse.InventorySummaryItem.builder()
                .id("total_warehouses")
                .label("Số kho vận (logic)")
                .value("1")
                .color(null)
                .build());

        list.add(ManagerReliefDashboardResponse.InventorySummaryItem.builder()
                .id("current_stock")
                .label("Tồn kho hiện tại")
                .value(totalQty.toPlainString())
                .color(null)
                .build());

        list.add(ManagerReliefDashboardResponse.InventorySummaryItem.builder()
                .id("imports_today")
                .label("Nhập trong ngày")
                .value("+" + todayImports.toPlainString())
                .color("text-blue-600")
                .build());

        list.add(ManagerReliefDashboardResponse.InventorySummaryItem.builder()
                .id("issues_today")
                .label("Xuất trong ngày")
                .value("-" + todayIssues.toPlainString())
                .color("text-red-500")
                .build());

        return list;
    }

    private List<ManagerReliefDashboardResponse.InventoryItem> buildInventoryItems() {
        List<StockBalanceEntity> balances = stockBalanceRepository.findAll();

        Map<ItemCategoryEntity, List<StockBalanceEntity>> byItem = balances.stream()
                .collect(Collectors.groupingBy(StockBalanceEntity::getItemCategory));

        return byItem.entrySet().stream()
                .map(entry -> {
                    ItemCategoryEntity item = entry.getKey();
                    BigDecimal donation = entry.getValue().stream()
                            .filter(b -> b.getSourceType() == StockSourceType.DONATION)
                            .map(StockBalanceEntity::getQty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal purchase = entry.getValue().stream()
                            .filter(b -> b.getSourceType() == StockSourceType.PURCHASE)
                            .map(StockBalanceEntity::getQty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal total = donation.add(purchase);

                    String status;
                    String statusColor;
                    if (total.compareTo(BigDecimal.ZERO) <= 0) {
                        status = "Hết hàng";
                        statusColor = "red";
                    } else if (total.compareTo(BigDecimal.valueOf(10)) < 0) {
                        status = "Sắp hết";
                        statusColor = "red";
                    } else {
                        status = "Còn hàng";
                        statusColor = "green";
                    }

                    return ManagerReliefDashboardResponse.InventoryItem.builder()
                            .code(item.getCode())
                            .name(item.getName())
                            .categoryName("Hàng cứu trợ") // placeholder
                            .unit(item.getUnit())
                            .qty(total)
                            .status(status)
                            .statusLabel(status)
                            .statusColor(statusColor)
                            .build();
                })
                .collect(Collectors.toList());
    }
}