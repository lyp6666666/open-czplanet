package com.ai.tutor.videocallimservice.chat.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息表
 * 对应表：message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    /** 消息id */
    private Long id;

    /** 会话id */
    private Long roomId;

    /** 发送者 user_id */
    private Long fromUid;

    /** 接收者 user_id */
    private Long toUid;

    /** 消息内容 */
    private String content;

    /** 是否命中屏蔽规则 0否 1是 */
    private Integer isMasked;

    /** 被回复的消息id */
    private Long replyMsgId;

    /** 状态 0正常 1删除 */
    private Integer status;

    /** 与被回复消息的间隔数 */
    private Integer gapCount;

    /** 消息类型 1文本 2撤回 */
    private Integer type;

    /** 扩展信息 */
    private String extra;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;
}
