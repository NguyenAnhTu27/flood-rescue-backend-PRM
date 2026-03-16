package com.floodrescue.module.relief.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DistributionTaskAssignRequest {

    private Long teamId;
    private Long assetId;
    private String status;
    private LocalDateTime eta;
    private String note;
}
