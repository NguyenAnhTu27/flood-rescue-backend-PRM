package com.floodrescue.module.rescue.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyRequest {

    @NotNull(message = "Trạng thái xác minh không được để trống")
    private Boolean locationVerified;

    private String note;

    private Boolean cancelRequest;

    // DELETE: hủy khỏi hệ thống xử lý, WAITING_TEAM: đưa lại hàng đợi với nhãn chờ đội
    private String cancelAction;

    private String cancelReason;
}
