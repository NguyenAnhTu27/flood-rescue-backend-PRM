package com.floodrescue.module.asset.dto.response;

import com.floodrescue.shared.enums.AssetStatus;
import com.floodrescue.shared.enums.AssetType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AssetResponse {
    private Long id;
    private String code;
    private String name;
    private AssetType assetType;
    private AssetStatus status;
    private Integer capacity;

    private Long assignedTeamId;
    private String assignedTeamName;

    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}