package com.floodrescue.shared.enums;

public enum RescueRequestStatus {
    PENDING,
    VERIFIED,
    ASSIGNED,       // Coordinator đã gán team, team chưa tới nơi
    IN_PROGRESS,    // Team đã tới nơi, đang thực hiện cứu hộ
    COMPLETED,
    CANCELLED,
    DUPLICATE
}
