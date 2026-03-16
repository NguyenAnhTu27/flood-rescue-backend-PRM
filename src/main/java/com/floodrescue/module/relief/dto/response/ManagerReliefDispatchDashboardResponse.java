package com.floodrescue.module.relief.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ManagerReliefDispatchDashboardResponse {

    private List<QueueItem> requests;
    private List<TeamItem> teams;
    private List<VehicleItem> vehicles;

    @Getter
    @Setter
    @Builder
    public static class QueueItem {
        private Long id;
        private String code;
        private String priority;
        private Integer peopleCount;
        private String timeAgo;
        private String status;
        private Boolean waitingForTeam;
        private Double lat;
        private Double lng;
    }

    @Getter
    @Setter
    @Builder
    public static class TeamItem {
        private Long id;
        private String name;
        private String area;
        private String status;
        private Double distance;
        private String lastUpdate;
        private Double lat;
        private Double lng;
        private Boolean online;
    }

    @Getter
    @Setter
    @Builder
    public static class VehicleItem {
        private Long id;
        private String code;
        private String name;
        private String type;
        private Integer capacity;
        private String status;
        private Double distance;
        private String location;
        private Boolean online;
    }
}
