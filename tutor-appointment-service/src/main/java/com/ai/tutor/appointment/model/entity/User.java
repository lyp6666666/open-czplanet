package com.ai.tutor.appointment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /** 用户id */
    private Long id;

    /** 用户昵称 */
    private String name;

    /** 手机号*/
    private String phone;

    /** 用户头像 */
    private String avatar;

    /** 性别 1为男性，2为女性 */
    private Integer sex;

    /** 微信openid用户标识 */
    private String openId;

    /** 在线状态 1在线 2离线 */
    private Integer activeStatus;

    /** 最后上下线时间 */
    private LocalDateTime lastOptTime;

    /** ip信息（JSON格式） */
    private String ipInfo;

    /** 佩戴的徽章id */
    private Long itemId;

    /** 使用状态 0.正常 1拉黑 */
    private Integer status;

    /** 用户类型 1教师 2学生 */
    private Integer userType;

    /** 逻辑外键，指向教师表或家长表id */
    private Long refId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;
}
