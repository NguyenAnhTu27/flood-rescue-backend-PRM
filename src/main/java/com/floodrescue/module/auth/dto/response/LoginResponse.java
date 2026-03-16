package com.floodrescue.module.auth.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    private String tokenType; // "Bearer"
    private Long userId;
    private String fullName;
    private String role; // CITIZEN/...
}