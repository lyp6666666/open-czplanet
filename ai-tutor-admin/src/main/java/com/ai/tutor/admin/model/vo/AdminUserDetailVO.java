package com.ai.tutor.admin.model.vo;

import com.ai.tutor.admin.model.entity.StudentProfile;
import com.ai.tutor.admin.model.entity.TeacherProfile;
import com.ai.tutor.admin.model.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserDetailVO {

    private User user;

    private TeacherProfile teacherProfile;

    private StudentProfile studentProfile;
}
