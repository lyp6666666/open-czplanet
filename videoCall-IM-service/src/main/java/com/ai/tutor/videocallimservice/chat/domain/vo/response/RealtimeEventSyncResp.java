package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "实时事件增量同步返回")
public class RealtimeEventSyncResp {

    @Schema(description = "下次继续同步时可直接复用的游标")
    private Long cursor;

    @Schema(description = "是否已经拉取到当前最新事件")
    private Boolean isLast;

    @Schema(description = "服务端当前已知的最新事件id")
    private Long latestEventId;

    @Schema(description = "本次返回的事件列表，按 eventId 升序")
    private List<RealtimeEventEnvelope> list;

    public static RealtimeEventSyncResp empty(Long cursor, Long latestEventId) {
        return RealtimeEventSyncResp.builder()
                .cursor(cursor)
                .isLast(true)
                .latestEventId(latestEventId)
                .list(new ArrayList<>())
                .build();
    }
}
