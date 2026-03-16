package com.floodrescue.module.rescue.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AssignmentResponse {

    private Long id;
    private Long taskGroupId;
    private String taskGroupCode;

    private Long teamId;
    private String teamName;

    private Long assetId;
    private String assetCode;
    private String assetName;

    private Long assignedById;
    private String assignedByName;

    private LocalDateTime assignedAt;
    private Boolean active;
}
