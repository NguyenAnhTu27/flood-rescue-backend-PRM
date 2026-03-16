package com.floodrescue.module.rescue.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BlockedCitizenResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private String blockedReason;
}
