package com.floodrescue.module.rescue.service;

import com.floodrescue.module.rescue.dto.response.RescueChatMessageResponse;

import java.util.List;

public interface RescueChatService {
    List<RescueChatMessageResponse> getMessages(Long rescueRequestId, Long userId);

    RescueChatMessageResponse sendMessage(Long rescueRequestId, Long userId, String message);
}
