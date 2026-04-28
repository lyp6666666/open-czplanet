package com.ai.tutor.liveclass.domain.vo.request;

import lombok.Data;

import java.util.Map;

@Data
public class LiveWhiteboardSnapshotRequest {
    private Long sceneVersion;
    private Map<String, Object> scene;
}
