package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.UserSimpleVO;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.integration.ImFacade;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.utils.ThrowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 联系人接口（供“创建日程/选择授课对象”使用）。
 *
 * <p>V1 仅提供：</p>
 * <ul>
 *   <li>recent：来自消息会话列表的“最近联系人”</li>
 *   <li>search：按昵称/手机号搜索用户</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/contacts")
@Tag(name = "联系人接口", description = "为约课创建提供授课对象选择能力")
public class ContactsController {

    @Resource
    private ImFacade imFacade;

    @Resource
    private UserMapper userMapper;

    @GetMapping("/recent")
    @Operation(summary = "最近联系人（来自会话列表）")
    public BaseResponse<List<UserSimpleVO>> recent(@RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        Long uid = RequestHolder.get().getUid();
        int safeLimit = Math.min(Math.max(limit == null ? 50 : limit, 1), 200);
        List<Long> otherUids = imFacade.listRecentContactUids(uid, safeLimit);
        if (otherUids == null || otherUids.isEmpty()) {
            return ResultUtils.success(Collections.emptyList());
        }
        List<User> users = userMapper.selectByIds(otherUids);
        if (users == null || users.isEmpty()) {
            return ResultUtils.success(Collections.emptyList());
        }
        List<UserSimpleVO> result = users.stream()
                .map(u -> UserSimpleVO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .avatar(u.getAvatar())
                        .userType(u.getUserType())
                        .build())
                .collect(Collectors.toList());
        return ResultUtils.success(result);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索联系人（昵称/手机号）")
    public BaseResponse<List<UserSimpleVO>> search(@RequestParam("q") String q,
                                                   @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        ThrowUtils.throwIf(q == null || q.isBlank(), ErrorCode.PARAMS_ERROR);
        int safeLimit = Math.min(Math.max(limit == null ? 50 : limit, 1), 200);
        List<User> users = userMapper.searchByKeyword(q.trim(), safeLimit);
        if (users == null || users.isEmpty()) {
            return ResultUtils.success(Collections.emptyList());
        }
        List<UserSimpleVO> result = users.stream()
                .map(u -> UserSimpleVO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .avatar(u.getAvatar())
                        .userType(u.getUserType())
                        .build())
                .collect(Collectors.toList());
        return ResultUtils.success(result);
    }
}

