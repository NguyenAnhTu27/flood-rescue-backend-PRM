package com.floodrescue.module.relief.dto.request;

import com.floodrescue.shared.enums.DistributionPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class DistributionVoucherCreateRequest {

    private String code;
    private Long issueId;
    private String issueRefCode;
    private Long reliefRequestId;
    private Long teamId;
    private Long assetId;
    private String receiverName;
    private String receiverPhone;
    private String deliveryAddress;
    private LocalDateTime eta;
    private DistributionPriority priority;
    private String note;

    @NotEmpty(message = "Danh sách dòng phiếu điều phối không được để trống")
    @Valid
    private List<DistributionVoucherLineRequest> lines;
}
