package com.ai.tutor.liveclass.domain.vo.request;

import lombok.Data;

@Data
public class UpdateLiveAiOptionsRequest {
    private Boolean realtimeSummaryEnabled;
    private Boolean postClassSummaryEnabled;
}
