package com.ai.tutor.videocallimservice.chat.domain.vo.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "通用游标翻页返回")
public class CursorPageResp<T> {

    @Schema(description = "下一页游标（下次翻页带上）")
    private Long cursor;

    @Schema(description = "是否最后一页")
    private Boolean isLast;

    @Schema(description = "数据列表")
    private List<T> list;

    public static <T> CursorPageResp<T> empty() {
        CursorPageResp<T> resp = new CursorPageResp<>();
        resp.setIsLast(true);
        resp.setList(new ArrayList<>());
        return resp;
    }
}

