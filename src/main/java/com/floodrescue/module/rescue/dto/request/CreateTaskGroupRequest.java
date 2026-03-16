package com.floodrescue.module.rescue.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateTaskGroupRequest {

    @NotEmpty(message = "Danh sách yêu cầu cứu hộ không được để trống")
    private List<Long> rescueRequestIds;

    // Optional: gán team ngay khi tạo group
    private Long assignedTeamId;

    private String note;
}
