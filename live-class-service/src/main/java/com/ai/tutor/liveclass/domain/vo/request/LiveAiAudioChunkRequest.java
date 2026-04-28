package com.ai.tutor.liveclass.domain.vo.request;

import lombok.Data;

@Data
public class LiveAiAudioChunkRequest {
    private Integer sequence;
    private Integer sampleRate;
    private Integer channelCount;
    private Integer durationMs;
    private Double rms;
    private String format;
    private String audioBase64;
}
