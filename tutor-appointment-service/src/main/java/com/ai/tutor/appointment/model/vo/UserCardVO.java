package com.ai.tutor.appointment.model.vo;

import com.ai.tutor.appointment.model.entity.StudentJobPosting;
import com.ai.tutor.appointment.model.entity.StudentProfile;
import com.ai.tutor.appointment.model.entity.TeacherProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCardVO {
    private UserSimpleVO user;
    private TeacherProfile teacherProfile;
    private StudentProfile studentProfile;
    private StudentJobPosting jobPosting;
}
