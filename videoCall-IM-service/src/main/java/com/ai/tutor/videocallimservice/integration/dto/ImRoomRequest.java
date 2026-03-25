package com.ai.tutor.videocallimservice.integration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ImRoomRequest {

    @NotNull
    private Long targetUid;
}
