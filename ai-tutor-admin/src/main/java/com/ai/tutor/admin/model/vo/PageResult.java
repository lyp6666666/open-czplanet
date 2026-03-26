package com.ai.tutor.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResult<T> {
    private List<T> records;
    private long total;
    private long size;
    private long current;
}
