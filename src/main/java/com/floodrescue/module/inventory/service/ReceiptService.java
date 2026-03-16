package com.floodrescue.module.inventory.service;

import com.floodrescue.module.inventory.dto.request.InventoryReceiptCreateRequest;
import com.floodrescue.module.inventory.dto.response.InventoryReceiptResponse;
import com.floodrescue.module.inventory.entity.InventoryReceiptEntity;
import com.floodrescue.module.inventory.entity.InventoryReceiptLineEntity;
import com.floodrescue.module.inventory.entity.ItemCategoryEntity;
import com.floodrescue.module.inventory.repository.ItemCategoryRepository;
import com.floodrescue.module.inventory.repository.ReceipRepository;
import com.floodrescue.module.inventory.repository.ReceiptLineRepository;
import com.floodrescue.shared.enums.InventoryDocumentStatus;
import com.floodrescue.shared.exception.BusinessException;
import com.floodrescue.shared.exception.NotFoundException;
import com.floodrescue.shared.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceipRepository receiptRepository;
    private final ReceiptLineRepository receiptLineRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final StockService stockService;

    @Transactional
    public InventoryReceiptResponse createReceipt(Long userId, InventoryReceiptCreateRequest request) {
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new BusinessException("Phiếu nhập phải có ít nhất 1 dòng");
        }

        String code = CodeGenerator.generateInventoryReceiptCode();

        final InventoryReceiptEntity receipt = InventoryReceiptEntity.builder()
                .code(code)
                .sourceType(request.getSourceType())
                .status(InventoryDocumentStatus.DRAFT)
                .createdById(userId)
                .note(request.getNote())
                .build();

        InventoryReceiptEntity saved = receiptRepository.save(receipt);

        List<InventoryReceiptLineEntity> lines = request.getLines().stream()
                .map(lineReq -> {
                    ItemCategoryEntity category = itemCategoryRepository.findById(lineReq.getItemCategoryId())
                            .orElseThrow(() -> new NotFoundException("Loại hàng không tồn tại: " + lineReq.getItemCategoryId()));
                    
                    return InventoryReceiptLineEntity.builder()
                            .receipt(saved)
                            .itemCategory(category)
                            .qty(BigDecimal.valueOf(lineReq.getQty()))
                            .unit(lineReq.getUnit())
                            .build();
                })
                .collect(Collectors.toList());

        receiptLineRepository.saveAll(lines);

        return toResponse(saved, lines);
    }

    @Transactional
    public InventoryReceiptResponse approveReceipt(Long id, Long userId) {
        InventoryReceiptEntity receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phiếu nhập không tồn tại"));

        if (receipt.getStatus() != InventoryDocumentStatus.DRAFT) {
            throw new BusinessException("Chỉ được duyệt phiếu ở trạng thái DRAFT");
        }

        List<InventoryReceiptLineEntity> lines = receiptLineRepository.findByReceipt(receipt);
        if (lines.isEmpty()) {
            throw new BusinessException("Phiếu nhập không có dòng nào");
        }

        // Cập nhật tồn kho
        stockService.applyReceipt(receipt, lines);

        receipt.setStatus(InventoryDocumentStatus.DONE);
        receipt = receiptRepository.save(receipt);

        return toResponse(receipt, lines);
    }

    @Transactional
    public InventoryReceiptResponse cancelReceipt(Long id, Long userId) {
        InventoryReceiptEntity receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phiếu nhập không tồn tại"));

        if (receipt.getStatus() != InventoryDocumentStatus.DRAFT) {
            throw new BusinessException("Chỉ được huỷ phiếu ở trạng thái DRAFT");
        }

        receipt.setStatus(InventoryDocumentStatus.CANCELLED);
        receipt = receiptRepository.save(receipt);

        List<InventoryReceiptLineEntity> lines = receiptLineRepository.findByReceipt(receipt);
        return toResponse(receipt, lines);
    }

    @Transactional(readOnly = true)
    public InventoryReceiptResponse getReceipt(Long id) {
        InventoryReceiptEntity receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phiếu nhập không tồn tại"));
        List<InventoryReceiptLineEntity> lines = receiptLineRepository.findByReceipt(receipt);
        return toResponse(receipt, lines);
    }

    @Transactional(readOnly = true)
    public Page<InventoryReceiptResponse> listReceipts(InventoryDocumentStatus status, Pageable pageable) {
        Page<InventoryReceiptEntity> page;
        if (status != null) {
            page = receiptRepository.findByStatus(status, pageable);
        } else {
            page = receiptRepository.findAll(pageable);
        }
        return page.map(r -> toResponse(r, receiptLineRepository.findByReceipt(r)));
    }

    private InventoryReceiptResponse toResponse(InventoryReceiptEntity receipt, List<InventoryReceiptLineEntity> lines) {
        return InventoryReceiptResponse.builder()
                .id(receipt.getId())
                .code(receipt.getCode())
                .sourceType(receipt.getSourceType())
                .status(receipt.getStatus())
                .createdById(receipt.getCreatedById())
                .note(receipt.getNote())
                .createdAt(receipt.getCreatedAt())
                .updatedAt(receipt.getUpdatedAt())
                .lines(lines.stream().map(l -> InventoryReceiptResponse.LineItem.builder()
                        .id(l.getId())
                        .itemCategoryId(l.getItemCategory().getId())
                        .itemCode(l.getItemCategory().getCode())
                        .itemName(l.getItemCategory().getName())
                        .qty(l.getQty())
                        .unit(l.getUnit())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}