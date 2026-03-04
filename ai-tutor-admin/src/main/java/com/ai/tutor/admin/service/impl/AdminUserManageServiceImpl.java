package com.ai.tutor.admin.service.impl;

import com.ai.tutor.admin.mapper.AdminUserManageMapper;
import com.ai.tutor.admin.model.dto.AdminUserCreateRequest;
import com.ai.tutor.admin.model.dto.AdminUserUpdateRequest;
import com.ai.tutor.admin.model.vo.AdminUserDetailVO;
import com.ai.tutor.admin.model.vo.AdminUserRowVO;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.appointment.mapper.StudentProfileMapper;
import com.ai.tutor.appointment.mapper.TeacherProfileMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.dto.user.BaseUserInfo;
import com.ai.tutor.appointment.model.dto.user.StudentExtInfo;
import com.ai.tutor.appointment.model.dto.user.TeacherExtInfo;
import com.ai.tutor.appointment.model.entity.StudentProfile;
import com.ai.tutor.appointment.model.entity.TeacherProfile;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class AdminUserManageServiceImpl implements com.ai.tutor.admin.service.AdminUserManageService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    @Resource
    private AdminUserManageMapper adminUserManageMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TeacherProfileMapper teacherProfileMapper;

    @Resource
    private StudentProfileMapper studentProfileMapper;

    @Override
    public PageResult<AdminUserRowVO> pageTeachers(String q, int page, int size) {
        long offset = (long) (page - 1) * size;
        return PageResult.<AdminUserRowVO>builder()
                .records(adminUserManageMapper.pageTeachers(q, offset, size))
                .total(adminUserManageMapper.countTeachers(q))
                .size(size)
                .current(page)
                .build();
    }

    @Override
    public PageResult<AdminUserRowVO> pageStudents(String q, int page, int size) {
        long offset = (long) (page - 1) * size;
        return PageResult.<AdminUserRowVO>builder()
                .records(adminUserManageMapper.pageStudents(q, offset, size))
                .total(adminUserManageMapper.countStudents(q))
                .size(size)
                .current(page)
                .build();
    }

    @Override
    public AdminUserDetailVO getDetail(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        User user = userMapper.selectById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        TeacherProfile tp = null;
        StudentProfile sp = null;
        if (user.getUserType() != null && user.getUserType() == 1) {
            tp = teacherProfileMapper.selectByUserId(id);
        } else if (user.getUserType() != null && user.getUserType() == 2) {
            sp = studentProfileMapper.selectByUserId(id);
        }
        return AdminUserDetailVO.builder()
                .user(user)
                .teacherProfile(tp)
                .studentProfile(sp)
                .build();
    }

    @Override
    public Long create(AdminUserCreateRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Integer userType = request.getUserType();
        ThrowUtils.throwIf(userType == null || (userType != 1 && userType != 2), ErrorCode.PARAMS_ERROR, "userType 仅支持 1教师 / 2学生");

        String phone = request.getPhone() == null ? null : request.getPhone().trim();
        ThrowUtils.throwIf(phone == null || !PHONE_PATTERN.matcher(phone).matches(), ErrorCode.PARAMS_ERROR, "手机号格式不正确");

        User exist = userMapper.selectByPhone(phone);
        ThrowUtils.throwIf(exist != null, ErrorCode.OPERATION_ERROR, "手机号已存在");

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .name(trimToNull(request.getName()))
                .phone(phone)
                .avatar(trimToNull(request.getAvatar()))
                .sex(request.getSex())
                .openId(null)
                .activeStatus(request.getActiveStatus() == null ? 2 : request.getActiveStatus())
                .lastOptTime(now)
                .ipInfo(null)
                .itemId(null)
                .status(request.getStatus() == null ? 0 : request.getStatus())
                .userType(userType)
                .refId(null)
                .createTime(now)
                .updateTime(now)
                .build();
        userMapper.insert(user);
        ThrowUtils.throwIf(user.getId() == null, ErrorCode.SYSTEM_ERROR, "创建用户失败");

        if (userType == 1) {
            TeacherProfile tp = TeacherProfile.builder()
                    .userId(user.getId())
                    .realName(trimToNull(request.getTeacherRealName()))
                    .education(trimToNull(request.getTeacherEducation()))
                    .subject(trimToNull(request.getTeacherSubject()))
                    .city(trimToNull(request.getTeacherCity()))
                    .ratePerHour(request.getTeacherRatePerHour())
                    .status(1)
                    .createTime(now)
                    .updateTime(now)
                    .build();
            teacherProfileMapper.insert(tp);
        } else {
            StudentProfile sp = StudentProfile.builder()
                    .userId(user.getId())
                    .realName(trimToNull(request.getStudentRealName()))
                    .age(request.getStudentAge())
                    .address(trimToNull(request.getStudentAddress()))
                    .demandDescription(trimToNull(request.getStudentDemandDescription()))
                    .budget(request.getStudentBudget())
                    .status(1)
                    .createTime(now)
                    .updateTime(now)
                    .build();
            studentProfileMapper.insert(sp);
        }

        return user.getId();
    }

    @Override
    public void update(Long id, AdminUserUpdateRequest request) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User user = userMapper.selectById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        if (request.getPhone() != null) {
            String newPhone = request.getPhone().trim();
            ThrowUtils.throwIf(!PHONE_PATTERN.matcher(newPhone).matches(), ErrorCode.PARAMS_ERROR, "手机号格式不正确");
            if (user.getPhone() == null || !newPhone.equals(user.getPhone())) {
                User exist = userMapper.selectByPhone(newPhone);
                ThrowUtils.throwIf(exist != null && exist.getId() != null && !exist.getId().equals(id), ErrorCode.OPERATION_ERROR, "手机号已存在");
                userMapper.updateUserPhone(newPhone, id);
            }
        }

        BaseUserInfo base = new BaseUserInfo();
        base.setName(trimToNull(request.getName()));
        base.setAvatar(trimToNull(request.getAvatar()));
        base.setSex(request.getSex());
        base.setStatus(request.getStatus());
        base.setActiveStatus(request.getActiveStatus());
        userMapper.updateUserBaseInfo(base, id);

        if (user.getUserType() != null && user.getUserType() == 1) {
            TeacherExtInfo ext = new TeacherExtInfo();
            ext.setRealName(trimToNull(request.getTeacherRealName()));
            ext.setEducation(trimToNull(request.getTeacherEducation()));
            ext.setSubject(trimToNull(request.getTeacherSubject()));
            ext.setCity(trimToNull(request.getTeacherCity()));
            ext.setRatePerHour(request.getTeacherRatePerHour());
            ext.setStatus(request.getTeacherProfileStatus());
            teacherProfileMapper.updateTeacherProfile(ext, id);
        } else if (user.getUserType() != null && user.getUserType() == 2) {
            StudentExtInfo ext = new StudentExtInfo();
            ext.setRealName(trimToNull(request.getStudentRealName()));
            ext.setAge(request.getStudentAge());
            ext.setAddress(trimToNull(request.getStudentAddress()));
            ext.setDemandDescription(trimToNull(request.getStudentDemandDescription()));
            ext.setBudget(request.getStudentBudget());
            ext.setStatus(request.getStudentProfileStatus());
            studentProfileMapper.updateStudentProfile(ext, id);
        }
    }

    @Override
    public void disable(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        User user = userMapper.selectById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        BaseUserInfo base = new BaseUserInfo();
        base.setStatus(1);
        userMapper.updateUserBaseInfo(base, id);

        if (user.getUserType() != null && user.getUserType() == 1) {
            TeacherExtInfo ext = new TeacherExtInfo();
            ext.setStatus(0);
            teacherProfileMapper.updateTeacherProfile(ext, id);
        } else if (user.getUserType() != null && user.getUserType() == 2) {
            StudentExtInfo ext = new StudentExtInfo();
            ext.setStatus(0);
            studentProfileMapper.updateStudentProfile(ext, id);
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
