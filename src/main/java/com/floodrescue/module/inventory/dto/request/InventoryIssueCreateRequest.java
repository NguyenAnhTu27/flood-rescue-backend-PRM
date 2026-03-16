package com.floodrescue.module.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InventoryIssueCreateRequest {

    // Liên kết với phiếu yêu cầu cứu trợ
    private Long reliefRequestId;

    // Đội vận chuyển / thực hiện xuất & giao
    private Long assignedTeamId;

    // Phương tiện
    private Long assetId;

    private String note;

    @NotEmpty(message = "Danh sách dòng phiếu xuất không được để trống")
    @Valid
    private List<InventoryIssueLineRequest> lines;
}