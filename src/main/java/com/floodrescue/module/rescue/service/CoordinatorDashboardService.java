package com.floodrescue.module.rescue.service;

import com.floodrescue.module.rescue.dto.response.CoordinatorDashboardResponse;

public interface CoordinatorDashboardService {

    CoordinatorDashboardResponse getDashboard(Long coordinatorId);
}

