package com.floodrescue.module.inventory.controller;

import com.floodrescue.module.inventory.dto.request.ItemCategoryCreateRequest;
import com.floodrescue.module.inventory.dto.request.ItemClassificationCreateRequest;
import com.floodrescue.module.inventory.dto.request.ItemUnitCreateRequest;
import com.floodrescue.module.inventory.dto.response.ItemCategoryResponse;
import com.floodrescue.module.inventory.dto.response.ItemClassificationResponse;
import com.floodrescue.module.inventory.dto.response.ItemUnitResponse;
import com.floodrescue.module.inventory.dto.response.StockBalanceItemResponse;
import com.floodrescue.module.inventory.entity.ItemCategoryEntity;
import com.floodrescue.module.inventory.entity.ItemClassificationEntity;
import com.floodrescue.module.inventory.entity.ItemUnitEntity;
import com.floodrescue.module.inventory.entity.StockBalanceEntity;
import com.floodrescue.module.inventory.repository.ItemCategoryRepository;
import com.floodrescue.module.inventory.repository.ItemClassificationRepository;
import com.floodrescue.module.inventory.repository.ItemUnitRepository;
import com.floodrescue.module.inventory.repository.StockBalanceRepository;
import com.floodrescue.shared.enums.StockSourceType;
import com.floodrescue.shared.exception.BusinessException;
import com.floodrescue.shared.exception.NotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class InventoryController {

    private final ItemCategoryRepository itemCategoryRepository;
    private final ItemClassificationRepository itemClassificationRepository;
    private final ItemUnitRepository itemUnitRepository;
    private final StockBalanceRepository stockBalanceRepository;

    @PostMapping("/items")
    public ResponseEntity<ItemCategoryResponse> createItemCategory(
            @Valid @RequestBody ItemCategoryCreateRequest request
    ) {
        if (itemCategoryRepository.existsByCode(request.getCode().trim())) {
            return ResponseEntity.badRequest().build();
        }

        ItemClassificationEntity classification = itemClassificationRepository.findById(request.getClassificationId())
                .orElseThrow(() -> new NotFoundException("Phân loại hàng không tồn tại"));
        ItemUnitEntity unit = itemUnitRepository.findAll().stream()
                .filter(u -> request.getUnit().trim().equalsIgnoreCase(u.getCode()) || request.getUnit().trim().equalsIgnoreCase(u.getName()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Đơn vị không tồn tại trong danh mục đơn vị"));

        ItemCategoryEntity entity = ItemCategoryEntity.builder()
                .code(request.getCode().trim())
                .name(request.getName().trim())
                .unit(unit.getCode())
                .classification(classification)
                .isActive(true)
                .build();

        entity = itemCategoryRepository.save(entity);

        return ResponseEntity.ok(toResponse(entity));
    }

    @GetMapping("/items")
    public ResponseEntity<List<ItemCategoryResponse>> getItemCategories(
            @RequestParam(required = false) Integer classificationId
    ) {
        List<ItemCategoryResponse> list = itemCategoryRepository.findAllWithClassification(classificationId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItemCategory(@PathVariable Integer id) {
        ItemCategoryEntity category = itemCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Danh mục hàng không tồn tại"));
        try {
            itemCategoryRepository.delete(category);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Không thể xóa danh mục đang phát sinh chứng từ nhập/xuất");
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/item-classifications")
    public ResponseEntity<ItemClassificationResponse> createItemClassification(
            @Valid @RequestBody ItemClassificationCreateRequest request
    ) {
        if (itemClassificationRepository.existsByCode(request.getCode().trim())) {
            throw new BusinessException("Mã phân loại đã tồn tại");
        }

        ItemClassificationEntity entity = ItemClassificationEntity.builder()
                .code(request.getCode().trim())
                .name(request.getName().trim())
                .isActive(true)
                .build();

        entity = itemClassificationRepository.save(entity);
        return ResponseEntity.ok(toClassificationResponse(entity));
    }

    @GetMapping("/item-classifications")
    public ResponseEntity<List<ItemClassificationResponse>> getItemClassifications() {
        List<ItemClassificationResponse> list = itemClassificationRepository.findAll().stream()
                .map(this::toClassificationResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/item-classifications/{id}")
    public ResponseEntity<Void> deleteItemClassification(@PathVariable Integer id) {
        if (itemCategoryRepository.countByClassification_Id(id) > 0) {
            throw new BusinessException("Không thể xóa phân loại đang được sử dụng trong danh mục hàng");
        }
        ItemClassificationEntity entity = itemClassificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phân loại hàng không tồn tại"));
        itemClassificationRepository.delete(entity);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/item-units")
    public ResponseEntity<ItemUnitResponse> createItemUnit(
            @Valid @RequestBody ItemUnitCreateRequest request
    ) {
        String code = request.getCode().trim().toUpperCase();
        String name = request.getName().trim();
        if (itemUnitRepository.existsByCodeOrName(code, name)) {
            throw new BusinessException("Đơn vị đã tồn tại");
        }
        ItemUnitEntity entity = itemUnitRepository.save(ItemUnitEntity.builder()
                .code(code)
                .name(name)
                .isActive(true)
                .build());
        return ResponseEntity.ok(toUnitResponse(entity));
    }

    @GetMapping("/item-units")
    public ResponseEntity<List<ItemUnitResponse>> getItemUnits() {
        List<ItemUnitResponse> list = itemUnitRepository.findAll().stream()
                .map(this::toUnitResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/item-units/{id}")
    public ResponseEntity<Void> deleteItemUnit(@PathVariable Integer id) {
        ItemUnitEntity entity = itemUnitRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Đơn vị không tồn tại"));
        if (itemCategoryRepository.countByUnitIgnoreCase(entity.getCode()) > 0) {
            throw new BusinessException("Không thể xóa đơn vị đang được sử dụng trong danh mục hàng");
        }
        itemUnitRepository.delete(entity);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stock")
    public ResponseEntity<List<StockBalanceItemResponse>> getStockBalances() {
        List<StockBalanceEntity> balances = stockBalanceRepository.findAll();

        Map<ItemCategoryEntity, List<StockBalanceEntity>> byItem = balances.stream()
                .collect(Collectors.groupingBy(StockBalanceEntity::getItemCategory));

        List<StockBalanceItemResponse> result = byItem.entrySet().stream()
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

                    return StockBalanceItemResponse.builder()
                            .itemCategoryId(item.getId())
                            .code(item.getCode())
                            .name(item.getName())
                            .unit(item.getUnit())
                            .donationQty(donation)
                            .purchaseQty(purchase)
                            .totalQty(total)
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    private ItemCategoryResponse toResponse(ItemCategoryEntity entity) {
        ItemClassificationEntity classification = entity.getClassification();
        return ItemCategoryResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .unit(entity.getUnit())
                .classificationId(classification == null ? null : classification.getId())
                .classificationCode(classification == null ? null : classification.getCode())
                .classificationName(classification == null ? null : classification.getName())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ItemClassificationResponse toClassificationResponse(ItemClassificationEntity entity) {
        return ItemClassificationResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ItemUnitResponse toUnitResponse(ItemUnitEntity entity) {
        return ItemUnitResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
