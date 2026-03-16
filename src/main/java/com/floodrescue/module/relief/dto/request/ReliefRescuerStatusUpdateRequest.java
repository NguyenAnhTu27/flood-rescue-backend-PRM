package com.floodrescue.module.relief.dto.request;

import com.floodrescue.shared.enums.ReliefDeliveryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReliefRescuerStatusUpdateRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private ReliefDeliveryStatus status;

    private String note;
}
