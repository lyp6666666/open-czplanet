package com.ai.tutor.liveclass.domain.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PrepareLiveSessionRequest {
    @NotBlank
    private String clientType;
    private String sourcePage;
    private Boolean realtimeSummaryEnabled;
    private Boolean postClassSummaryEnabled;
}
