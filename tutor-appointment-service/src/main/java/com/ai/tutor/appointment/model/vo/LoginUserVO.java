package com.ai.tutor.appointment.model.vo;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;

@Data
@Builder
public class LoginUserVO implements Serializable {

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

    /** 用户类型 1教师 2学生 */
    private Integer userType;


    private static final long serialVersionUID = 1L;
}
