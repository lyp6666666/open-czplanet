package com.ai.tutor.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminLoginResponse {
    private String token;
    private String nickname;
    private Long id;
}
