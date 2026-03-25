package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.service.ContactQueryService;
import com.ai.tutor.appointment.model.vo.UserSimpleVO;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    private ContactQueryService contactQueryService;

    @GetMapping("/recent")
    @Operation(summary = "最近联系人（来自会话列表）")
    public BaseResponse<List<UserSimpleVO>> recent(@RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(contactQueryService.recentContacts(uid, limit));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索联系人（昵称/手机号）")
    public BaseResponse<List<UserSimpleVO>> search(@RequestParam("q") String q,
                                                   @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        Long uid = RequestHolder.get() == null ? null : RequestHolder.get().getUid();
        Integer role = RequestHolder.get() == null ? null : RequestHolder.get().getRole();
        return ResultUtils.success(contactQueryService.searchContacts(uid, role, q, limit));
    }
}
