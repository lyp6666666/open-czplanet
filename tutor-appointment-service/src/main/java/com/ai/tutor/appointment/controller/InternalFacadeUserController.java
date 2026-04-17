package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.service.UserReadService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.utils.ResultUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/internal/facade/users")
@RequiredArgsConstructor
public class InternalFacadeUserController {

    private final UserMapper userMapper;
    private final UserReadService userReadService;

    @GetMapping("/{uid}/basic")
    public BaseResponse<Map<String, Object>> getUserBasicById(@PathVariable("uid") Long uid) {
        if (uid == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("userType", user.getUserType());
        info.put("refId", user.getRefId());
        info.put("status", user.getStatus());
        return ResultUtils.success(info);
    }

    @GetMapping("/{uid}/phone")
    public BaseResponse<String> getUserPhoneById(@PathVariable("uid") Long uid) {
        if (uid == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(userReadService.getPhoneByUserId(uid));
    }
}
