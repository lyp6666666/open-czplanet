package com.ai.tutor.liveclass.domain.vo.request;

import lombok.Data;

@Data
public class LiveDeviceReportRequest {
    private String reportStage;
    private String cameraStatus;
    private String micStatus;
    private String speakerStatus;
    private String networkLevel;
    private String browserInfo;
    private String osInfo;
    private Object deviceInfo;
}
