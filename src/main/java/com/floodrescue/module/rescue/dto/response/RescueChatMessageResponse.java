package com.floodrescue.module.rescue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescueChatMessageResponse {
    private Long id;
    private Long rescueRequestId;
    private Long senderId;
    private String senderName;
    private String senderRole;
    private String message;
    private LocalDateTime createdAt;
}
