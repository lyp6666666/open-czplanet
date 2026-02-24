package com.ai.tutor.videocallimservice.chat.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.UnlockedContactVO;
import com.ai.tutor.videocallimservice.chat.service.ContactUnlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat/contact")
@Tag(name = "联系方式解锁接口", description = "中介费支付后查看对方联系方式")
public class ContactUnlockController {

    @Resource
    private ContactUnlockService contactUnlockService;

    @GetMapping("/unlock")
    @Operation(summary = "查看对方联系方式（需支付完成）")
    public BaseResponse<UnlockedContactVO> unlock(@RequestParam("roomId") Long roomId, @RequestParam("targetUid") Long targetUid) {
        Long uid = RequestHolder.get().getUid();
        return ResultUtils.success(contactUnlockService.getUnlockedContact(roomId, targetUid, uid));
    }
}
