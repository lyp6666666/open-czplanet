package com.ai.tutor.appointment.model.vo;

import com.ai.tutor.appointment.model.entity.StudentProfile;
import com.ai.tutor.appointment.model.entity.TeacherProfile;
import com.ai.tutor.appointment.model.entity.OrganizationProfile;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class UserMeVO implements Serializable {

    private Long id;

    private String name;

    private String phone;

    private String avatar;

    private Integer sex;

    private Integer userType;

    private TeacherProfile teacherProfile;

    private StudentProfile studentProfile;

    private OrganizationProfile organizationProfile;

    private static final long serialVersionUID = 1L;
}
