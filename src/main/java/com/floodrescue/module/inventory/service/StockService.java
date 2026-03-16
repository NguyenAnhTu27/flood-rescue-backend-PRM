package com.floodrescue.module.inventory.service;

import com.floodrescue.module.inventory.entity.InventoryIssueLineEntity;
import com.floodrescue.module.inventory.entity.InventoryReceiptEntity;
import com.floodrescue.module.inventory.entity.InventoryReceiptLineEntity;
import com.floodrescue.module.inventory.entity.StockBalanceEntity;
import com.floodrescue.module.inventory.repository.StockBalanceRepository;
import com.floodrescue.shared.enums.StockSourceType;
import com.floodrescue.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockBalanceRepository stockBalanceRepository;

    @Transactional
    public void applyReceipt(InventoryReceiptEntity receipt, java.util.List<InventoryReceiptLineEntity> lines) {
        StockSourceType sourceType = receipt.getSourceType();
        for (InventoryReceiptLineEntity line : lines) {
            BigDecimal qty = line.getQty();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) continue;

            StockBalanceEntity balance = stockBalanceRepository
                    .findByItemCategoryAndSourceType(line.getItemCategory(), sourceType)
                    .orElseGet(() -> StockBalanceEntity.builder()
                            .itemCategory(line.getItemCategory())
                            .sourceType(sourceType)
                            .qty(BigDecimal.ZERO)
                            .build());

            balance.setQty(balance.getQty().add(qty));
            stockBalanceRepository.save(balance);
        }
    }

    @Transactional
    public void applyIssue(java.util.List<InventoryIssueLineEntity> lines) {
        for (InventoryIssueLineEntity line : lines) {
            BigDecimal need = line.getQty();
            if (need == null || need.compareTo(BigDecimal.ZERO) <= 0) continue;

            var balances = stockBalanceRepository.findByItemCategory(line.getItemCategory());
            BigDecimal total = balances.stream()
                    .map(StockBalanceEntity::getQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (total.compareTo(need) < 0) {
                throw new BusinessException("Không đủ tồn kho cho mặt hàng: " + line.getItemCategory().getName());
            }

            // Trừ lần lượt theo từng nguồn
            BigDecimal remaining = need;
            for (StockBalanceEntity b : balances) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal available = b.getQty();
                BigDecimal toUse = available.min(remaining);
                b.setQty(available.subtract(toUse));
                remaining = remaining.subtract(toUse);
                stockBalanceRepository.save(b);
            }
        }
    }
}