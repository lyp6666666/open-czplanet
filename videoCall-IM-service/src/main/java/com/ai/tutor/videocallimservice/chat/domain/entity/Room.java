package com.ai.tutor.videocallimservice.chat.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生与教师之间的1对1会话表
 * 对应表：room
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    /** 会话id */
    private Long id;

    /** 教师资料id（逻辑外键） */
    private Long teacherProfileId;

    /** 学生资料id（逻辑外键） */
    private Long studentProfileId;

    /** 最后消息时间 */
    private LocalDateTime activeTime;

    /** 最后一条消息id */
    private Long lastMsgId;

    /** 状态 1正常 0关闭/归档 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;
}