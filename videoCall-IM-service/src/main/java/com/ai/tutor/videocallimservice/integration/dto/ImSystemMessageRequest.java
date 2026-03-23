package com.ai.tutor.videocallimservice.integration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ImSystemMessageRequest {

    @NotNull
    private Long roomId;

    @NotNull
    private Object body;
}
