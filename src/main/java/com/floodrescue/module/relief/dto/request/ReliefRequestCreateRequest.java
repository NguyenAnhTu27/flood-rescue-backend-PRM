package com.floodrescue.module.relief.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReliefRequestCreateRequest {

    @NotBlank(message = "Khu vực cứu trợ không được để trống")
    private String targetArea;

    private String addressText;

    private Double latitude;

    private Double longitude;

    private String locationDescription;

    // Có thể null nếu tạo yêu cầu cứu trợ độc lập, không gắn với yêu cầu cứu nạn cụ thể
    private Long rescueRequestId;

    private String note;

    @Valid
    private List<ReliefRequestLineRequest> lines;
}
