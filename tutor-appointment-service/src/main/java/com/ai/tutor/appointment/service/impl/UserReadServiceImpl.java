package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.mapper.OrganizationProfileMapper;
import com.ai.tutor.appointment.mapper.StudentJobPostingMapper;
import com.ai.tutor.appointment.mapper.StudentProfileMapper;
import com.ai.tutor.appointment.mapper.TeacherProfileMapper;
import com.ai.tutor.appointment.mapper.TutorAppointmentMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.entity.StudentProfile;
import com.ai.tutor.appointment.model.entity.TeacherProfile;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.UserCardVO;
import com.ai.tutor.appointment.model.vo.UserMeVO;
import com.ai.tutor.appointment.model.vo.UserSimpleVO;
import com.ai.tutor.appointment.service.UserReadService;
import com.ai.tutor.appointment.storage.MinioProperties;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserReadServiceImpl implements UserReadService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private TeacherProfileMapper teacherProfileMapper;
    @Resource
    private StudentProfileMapper studentProfileMapper;
    @Resource
    private OrganizationProfileMapper organizationProfileMapper;
    @Resource
    private StudentJobPostingMapper studentJobPostingMapper;
    @Resource
    private ObjectProvider<TutorAppointmentMapper> tutorAppointmentMapperProvider;
    @Resource
    private MinioProperties minioProperties;

    private static final String DEFAULT_AVATAR_PATH = "/avatars/default-avatar.svg";

    @Override
    public UserMeVO getMe(Long userId) {
        User user = userMapper.selectById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        UserRoleEnum role = UserRoleEnum.fromValue(user.getUserType());
        UserMeVO.UserMeVOBuilder builder = UserMeVO.builder()
                .id(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .avatar(normalizeAvatar(user.getAvatar()))
                .sex(user.getSex())
                .userType(user.getUserType());
        if (role == UserRoleEnum.TEACHER) {
            builder.teacherProfile(teacherProfileMapper.selectByUserId(userId));
        } else if (role == UserRoleEnum.STUDENT) {
            builder.studentProfile(studentProfileMapper.selectByUserId(userId));
        } else if (role == UserRoleEnum.ORG) {
            builder.organizationProfile(organizationProfileMapper.selectByUserId(userId));
        }
        return builder.build();
    }

    @Override
    public List<UserSimpleVO> batch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> users = userMapper.selectByIds(ids);
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> realNameByUserId = loadRealNameMap(users);
        return users.stream()
                .map(u -> UserSimpleVO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .realName(realNameByUserId.get(u.getId()))
                        .avatar(normalizeAvatar(u.getAvatar()))
                        .userType(u.getUserType())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public UserCardVO getUserCard(Long currentUserId, Long targetUserId) {
        User user = userMapper.selectById(targetUserId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        UserRoleEnum role = UserRoleEnum.fromValue(user.getUserType());
        UserCardVO.UserCardVOBuilder builder = UserCardVO.builder()
                .user(UserSimpleVO.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .realName(resolveRealName(user))
                        .avatar(normalizeAvatar(user.getAvatar()))
                        .userType(user.getUserType())
                        .build());

        if (role == UserRoleEnum.TEACHER) {
            builder.teacherProfile(teacherProfileMapper.selectByUserId(targetUserId));
            TutorAppointmentMapper appt = tutorAppointmentMapperProvider == null ? null : tutorAppointmentMapperProvider.getIfAvailable();
            if (appt != null) {
                builder.teacherHistory(appt.listByUser(targetUserId, 5, null, 20));
            }
        } else if (role == UserRoleEnum.STUDENT) {
            builder.studentProfile(studentProfileMapper.selectByUserId(targetUserId));
            builder.jobPosting(studentJobPostingMapper.selectLatestPublishedByParentId(targetUserId));
            builder.studentHistory(studentJobPostingMapper.listByParentId(targetUserId, null, 20));
        }

        return builder.build();
    }

    @Override
    public String getPhoneByUserId(Long userId) {
        User user = userMapper.selectById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return user.getPhone();
    }

    @Override
    public void ensurePhoneNotOccupiedByOther(String phone, Long currentUserId) {
        User occupied = userMapper.selectByPhone(phone);
        ThrowUtils.throwIf(occupied != null && !occupied.getId().equals(currentUserId), ErrorCode.OPERATION_ERROR, "手机号已被占用");
    }

    private String resolveDefaultAvatarUrl() {
        if (minioProperties != null && minioProperties.isEnabled()) {
            String publicBaseUrl = minioProperties.getPublicBaseUrl();
            String objectKey = minioProperties.getDefaultAvatarObjectKey();
            if (publicBaseUrl != null && !publicBaseUrl.trim().isEmpty() && objectKey != null && !objectKey.trim().isEmpty()) {
                String base = publicBaseUrl.trim();
                String key = objectKey.trim();
                if (key.startsWith("/")) {
                    key = key.substring(1);
                }
                if (base.endsWith("/")) {
                    return base + key;
                }
                return base + "/" + key;
            }
        }
        return DEFAULT_AVATAR_PATH;
    }

    private String normalizeAvatar(String avatar) {
        if (avatar != null && !avatar.trim().isEmpty()) {
            return avatar;
        }
        return resolveDefaultAvatarUrl();
    }

    private Map<Long, String> loadRealNameMap(List<User> users) {
        Map<Long, String> out = new HashMap<>();
        for (User user : users) {
            if (user == null || user.getId() == null) {
                continue;
            }
            String realName = resolveRealName(user);
            if (realName != null) {
                out.put(user.getId(), realName);
            }
        }
        return out;
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
