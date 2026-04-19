package com.ai.tutor.liveclass.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveClassDeviceReport {
    private Long id;
    private Long sessionId;
    private Long uid;
    private String reportStage;
    private String cameraStatus;
    private String micStatus;
    private String speakerStatus;
    private String networkLevel;
    private String browserInfo;
    private String osInfo;
    private String deviceJson;
    private LocalDateTime createTime;
}
