package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.mapper.StudentProfileMapper;
import com.ai.tutor.appointment.mapper.TeacherProfileMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.entity.StudentProfile;
import com.ai.tutor.appointment.model.entity.TeacherProfile;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.UserSimpleVO;
import com.ai.tutor.appointment.service.ContactQueryService;
import com.ai.tutor.common.integration.ImFacade;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContactQueryServiceImpl implements ContactQueryService {

    @Resource
    private ImFacade imFacade;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TeacherProfileMapper teacherProfileMapper;

    @Resource
    private StudentProfileMapper studentProfileMapper;

    @Override
    public List<UserSimpleVO> recentContacts(Long uid, Integer limit) {
        int safeLimit = Math.min(Math.max(limit == null ? 50 : limit, 1), 200);
        List<Long> otherUids = imFacade.listRecentContactUids(uid, safeLimit);
        if (otherUids == null || otherUids.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> users = userMapper.selectByIds(otherUids);
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        return users.stream()
                .map(u -> UserSimpleVO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .realName(resolveRealName(u))
                        .avatar(u.getAvatar())
                        .userType(u.getUserType())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<UserSimpleVO> searchContacts(Long uid, Integer role, String keyword, Integer limit) {
        ThrowUtils.throwIf(keyword == null || keyword.isBlank(), ErrorCode.PARAMS_ERROR);
        final Integer expectUserType = Integer.valueOf(1).equals(role) ? 2 : (Integer.valueOf(2).equals(role) ? 1 : null);
        int safeLimit = Math.min(Math.max(limit == null ? 50 : limit, 1), 200);
        List<User> users = userMapper.searchByKeyword(keyword.trim(), safeLimit);
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        return users.stream()
                .filter(u -> u != null)
                .filter(u -> uid == null || u.getId() == null || !u.getId().equals(uid))
                .filter(u -> expectUserType == null || (u.getUserType() != null && u.getUserType().equals(expectUserType)))
                .map(u -> UserSimpleVO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .realName(resolveRealName(u))
                        .avatar(u.getAvatar())
                        .userType(u.getUserType())
                        .build())
                .collect(Collectors.toList());
    }

    private String resolveRealName(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        UserRoleEnum role = UserRoleEnum.fromValue(user.getUserType());
        if (role == UserRoleEnum.TEACHER) {
            TeacherProfile profile = teacherProfileMapper.selectByUserId(user.getId());
            return normalizeRealName(profile == null ? null : profile.getRealName());
        }
        if (role == UserRoleEnum.STUDENT) {
            StudentProfile profile = studentProfileMapper.selectByUserId(user.getId());
            return normalizeRealName(profile == null ? null : profile.getRealName());
        }
        return null;
    }

    private String normalizeRealName(String realName) {
        if (realName == null) {
            return null;
        }
        String trimmed = realName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
