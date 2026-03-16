package com.floodrescue.module.rescue.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class RescuerDashboardResponse {
    private Long teamId;
    private String teamName;
    private Double teamLatitude;
    private Double teamLongitude;
    private String teamLocationText;
    private LocalDateTime teamLocationUpdatedAt;

    private Long activeTaskGroups;
    private Long activeAssignments;
    private List<HeldAssetItem> heldAssets;

    // Danh sách nhóm nhiệm vụ gần nhất/đang hoạt động của đội
    private List<TaskGroupResponse> taskGroups;

    @Getter
    @Setter
    @Builder
    public static class HeldAssetItem {
        private Long id;
        private String code;
        private String name;
        private String assetType;
        private String status;
    }
}
