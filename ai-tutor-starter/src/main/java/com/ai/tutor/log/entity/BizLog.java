package com.ai.tutor.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("biz_log")
public class BizLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String module;

    private String traceId;

    private String keyword;

    private String level;

    private String content;

    /** 业务时间戳 */
    private LocalDateTime createdAt;
}
