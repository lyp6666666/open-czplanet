package com.ai.tutor.appointment.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.ai.tutor.appointment.enums.UserRoleEnum;
import com.ai.tutor.appointment.mapper.OrganizationAccountMapper;
import com.ai.tutor.appointment.mapper.OrganizationProfileMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.dto.organization.OrgChangePasswordRequest;
import com.ai.tutor.appointment.model.dto.organization.OrgLoginRequest;
import com.ai.tutor.appointment.model.entity.OrganizationAccount;
import com.ai.tutor.appointment.model.entity.OrganizationProfile;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.OrgLoginVO;
import com.ai.tutor.appointment.service.OrganizationAuthService;
import com.ai.tutor.appointment.utils.JwtUtil;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class OrganizationAuthServiceImpl implements OrganizationAuthService {

    @Resource
    private OrganizationAccountMapper organizationAccountMapper;

    @Resource
    private OrganizationProfileMapper organizationProfileMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public OrgLoginVO login(OrgLoginRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        String password = request.getPassword() == null ? null : request.getPassword().trim();
        ThrowUtils.throwIf(username == null || username.isEmpty() || password == null || password.isEmpty(), ErrorCode.PARAMS_ERROR);

        OrganizationAccount account = organizationAccountMapper.selectByUsername(username);
        ThrowUtils.throwIf(account == null, ErrorCode.NOT_FOUND_ERROR, "机构账号不存在");
        ThrowUtils.throwIf(account.getStatus() != null && account.getStatus() == 0, ErrorCode.NO_AUTH_ERROR, "机构账号已禁用");

        boolean ok = BCrypt.checkpw(password, account.getPasswordHash());
        ThrowUtils.throwIf(!ok, ErrorCode.PARAMS_ERROR, "账号或密码错误");

        User user = userMapper.selectById(account.getOrgUserId());
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "机构用户不存在");
        ThrowUtils.throwIf(user.getUserType() == null || user.getUserType() != UserRoleEnum.ORG.getValue(), ErrorCode.NO_AUTH_ERROR, "账号类型不匹配");
        ThrowUtils.throwIf(user.getStatus() != null && user.getStatus() == 1, ErrorCode.NO_AUTH_ERROR, "账号已被拉黑");

        OrganizationProfile profile = organizationProfileMapper.selectByUserId(user.getId());
        ThrowUtils.throwIf(profile == null, ErrorCode.NOT_FOUND_ERROR, "机构资料未初始化，请联系管理员");

        String subject = user.getPhone() == null ? ("org:" + user.getId()) : user.getPhone();
        String token = jwtUtil.generateToken(user.getId(), subject, UserRoleEnum.ORG);

        try {
            organizationAccountMapper.updateLastLogin(user.getId());
        } catch (Exception ignored) {
        }

        return OrgLoginVO.builder()
                .id(user.getId())
                .name(profile.getOrgName() == null || profile.getOrgName().trim().isEmpty() ? (user.getName() == null ? "机构" : user.getName()) : profile.getOrgName())
                .userType(user.getUserType())
                .token(token)
                .mustChangePassword(account.getMustChangePassword() != null && account.getMustChangePassword() == 1)
                .organizationProfile(profile)
                .build();
    }

    @Override
    public void changePassword(Long orgUserId, OrgChangePasswordRequest request) {
        ThrowUtils.throwIf(orgUserId == null || request == null, ErrorCode.PARAMS_ERROR);
        String oldPassword = request.getOldPassword() == null ? null : request.getOldPassword().trim();
        String newPassword = request.getNewPassword() == null ? null : request.getNewPassword().trim();
        ThrowUtils.throwIf(oldPassword == null || oldPassword.isEmpty(), ErrorCode.PARAMS_ERROR, "旧密码不能为空");
        ThrowUtils.throwIf(newPassword == null || newPassword.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "新密码不能为空");
        ThrowUtils.throwIf(newPassword.trim().length() < 8, ErrorCode.PARAMS_ERROR, "新密码至少 8 位");

        OrganizationAccount account = organizationAccountMapper.selectByOrgUserId(orgUserId);
        ThrowUtils.throwIf(account == null, ErrorCode.NOT_FOUND_ERROR, "机构账号不存在");
        ThrowUtils.throwIf(account.getStatus() != null && account.getStatus() == 0, ErrorCode.NO_AUTH_ERROR, "机构账号已禁用");

        boolean ok = BCrypt.checkpw(oldPassword, account.getPasswordHash());
        ThrowUtils.throwIf(!ok, ErrorCode.PARAMS_ERROR, "旧密码不正确");

        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        int updated = organizationAccountMapper.updatePassword(orgUserId, newHash, 0);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改失败");
        }
    }
}
