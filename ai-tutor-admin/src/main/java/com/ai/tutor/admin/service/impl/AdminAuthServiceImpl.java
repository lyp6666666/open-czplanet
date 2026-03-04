package com.ai.tutor.admin.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.ai.tutor.admin.mapper.SysAdminUserMapper;
import com.ai.tutor.admin.model.dto.AdminLoginRequest;
import com.ai.tutor.admin.model.entity.SysAdminUser;
import com.ai.tutor.admin.model.vo.AdminLoginResponse;
import com.ai.tutor.admin.service.AdminAuthService;
import com.ai.tutor.admin.utils.JwtUtil;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    @Resource
    private SysAdminUserMapper sysAdminUserMapper;

    @Resource(name = "adminJwtUtil")
    private JwtUtil jwtUtil;

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        String password = request.getPassword();

        ThrowUtils.throwIf(username == null || password == null, ErrorCode.PARAMS_ERROR);

        SysAdminUser user = sysAdminUserMapper.selectOne(new LambdaQueryWrapper<SysAdminUser>()
                .eq(SysAdminUser::getUsername, username));

        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "管理员账号不存在，请先导入 seed_dev_data.sql 初始化数据");
        ThrowUtils.throwIf(!BCrypt.checkpw(password, user.getPassword()), ErrorCode.PARAMS_ERROR, "用户名或密码错误");

        ThrowUtils.throwIf(user.getStatus() != null && user.getStatus() == 0, ErrorCode.NO_AUTH_ERROR, "账号已禁用");

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        return AdminLoginResponse.builder()
                .token(token)
                .id(user.getId())
                .nickname(user.getNickname())
                .build();
    }
}
