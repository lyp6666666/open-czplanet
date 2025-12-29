package com.ai.tutor.videocallimservice.common.util;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.CursorPageBaseReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageBaseResp;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 游标分页工具页
 */
public class CursorUtils {

    public static <T> CursorPageBaseResp<T> getCursorPageByMysql(
            IService<T> service,
            CursorPageBaseReq request,
            Consumer<LambdaQueryWrapper<T>> initWrapper,
            SFunction<T, ?> cursorColumn
    ) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();

        // 1. 额外查询条件
        initWrapper.accept(wrapper);

        // 2. 游标条件（只支持 Long 型 cursor）
        if (StrUtil.isNotBlank(request.getCursor())) {
            Long cursor = Long.parseLong(request.getCursor());
            wrapper.lt(cursorColumn, cursor);
        }

        // 3. 排序方向（倒序）
        wrapper.orderByDesc(cursorColumn);

        // 4. 查询
        Page<T> page = service.page(request.plusPage(), wrapper);

        // 5. 计算下一页 cursor
        String nextCursor = Optional.ofNullable(CollectionUtil.getLast(page.getRecords()))
                .map(cursorColumn)
                .map(Object::toString)
                .orElse(null);

        // 6. 是否最后一页
        boolean isLast = page.getRecords().size() < request.getPageSize();

        return new CursorPageBaseResp<>(nextCursor, isLast, page.getRecords());
    }
}
