package com.floodrescue.module.rescue.dto.request;

import com.floodrescue.shared.enums.RescuePriority;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrioritizeRequest {

    @NotNull(message = "Mức độ ưu tiên không được để trống")
    private RescuePriority priority;
}
