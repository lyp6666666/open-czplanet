package com.ai.tutor.videocallimservice.integration.controller;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.event.PaymentSuccessEvent;
import com.ai.tutor.common.integration.BrokerageOrderPayInfo;
import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatMessageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.request.ChatRoomPageReq;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.ChatRoomItemResp;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.CursorPageResp;
import com.ai.tutor.videocallimservice.chat.service.BrokerageOrderService;
import com.ai.tutor.videocallimservice.chat.service.ChatRoomService;
import com.ai.tutor.videocallimservice.chat.service.ChatService;
import com.ai.tutor.videocallimservice.chat.service.CourseEnrollmentService;
import com.ai.tutor.videocallimservice.chat.service.TutorApplicationService;
import com.ai.tutor.videocallimservice.integration.dto.ImRoomRequest;
import com.ai.tutor.videocallimservice.integration.dto.ImSystemMessageRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@RestController
@RequestMapping("/internal/facade")
@RequiredArgsConstructor
public class InternalFacadeController {

    private static final int DEFAULT_CONTACT_LIMIT = 20;
    private static final int MAX_CONTACT_LIMIT = 100;

    private final ChatRoomService chatRoomService;
    private final ChatService chatService;
    private final BrokerageOrderService brokerageOrderService;
    private final TutorApplicationService tutorApplicationService;
    private final CourseEnrollmentService courseEnrollmentService;

    @PostMapping("/im/rooms/with-user")
    public BaseResponse<Long> getOrCreateRoomWithUser(@Valid @RequestBody ImRoomRequest request) {
        Long uid = requireUid();
        Long roomId = chatRoomService.getOrCreateRoomWithUser(request.getTargetUid(), uid);
        return ResultUtils.success(roomId);
    }

    @PostMapping("/im/messages/system")
    public BaseResponse<Long> sendSystemMessage(@Valid @RequestBody ImSystemMessageRequest request) {
        Long uid = requireUid();
        ChatMessageReq msgReq = ChatMessageReq.builder()
                .roomId(request.getRoomId())
                .msgType(8)
                .body(request.getBody())
                .build();
        return ResultUtils.success(chatService.sendMsg(msgReq, uid));
    }

    @PostMapping("/im/rooms/{roomId}/schedule-ready")
    public BaseResponse<Boolean> assertRoomReadyForScheduling(@PathVariable("roomId") Long roomId) {
        Long uid = requireUid();
        tutorApplicationService.assertRoomReadyForScheduling(roomId, uid);
        return ResultUtils.success(Boolean.TRUE);
    }

    @GetMapping("/im/contacts/recent")
    public BaseResponse<List<Long>> listRecentContactUids(@RequestParam(value = "limit", required = false) Integer limit) {
        Long uid = requireUid();
        int safeLimit = normalizeLimit(limit);

        ChatRoomPageReq pageReq = new ChatRoomPageReq();
        pageReq.setPageSize(safeLimit);
        pageReq.setCursor(null);

        CursorPageResp<ChatRoomItemResp> page = chatRoomService.listRooms(pageReq, uid);
        if (page == null || page.getList() == null || page.getList().isEmpty()) {
            return ResultUtils.success(List.of());
        }

        LinkedHashSet<Long> ordered = new LinkedHashSet<>();
        for (ChatRoomItemResp item : page.getList()) {
            if (item == null || item.getOtherUid() == null) {
                continue;
            }
            ordered.add(item.getOtherUid());
            if (ordered.size() >= safeLimit) {
                break;
            }
        }
        return ResultUtils.success(new ArrayList<>(ordered));
    }

    @PostMapping("/courses/{courseId}/weekly-schedule-submitted")
    public BaseResponse<Boolean> confirmWeeklyScheduleSubmitted(@PathVariable("courseId") Long courseId,
                                                                @RequestBody ConfirmWeeklyScheduleRequest request) {
        Long uid = requireUid();
        courseEnrollmentService.confirmWeeklyScheduleSubmitted(
                courseId,
                uid,
                request == null ? null : request.getClassTime(),
                request == null ? null : request.getFrequencyPerWeek(),
                request == null ? null : request.getLessonPriceFen()
        );
        return ResultUtils.success(Boolean.TRUE);
    }

    @PostMapping("/courses/{courseId}/trial-canceled")
    public BaseResponse<Boolean> markTrialCanceled(@PathVariable("courseId") Long courseId,
                                                   @RequestBody TrialCanceledRequest request) {
        Long uid = requireUid();
        courseEnrollmentService.markTrialCanceled(
                courseId,
                uid,
                request == null ? null : request.getReason()
        );
        return ResultUtils.success(Boolean.TRUE);
    }

    @GetMapping("/brokerage/orders/{orderId}/payable")
    public BaseResponse<BrokerageOrderPayInfo> getPayableOrder(@PathVariable("orderId") Long orderId,
                                                                @RequestParam("uid") Long uid) {
        Long requesterUid = requireUid();
        ThrowUtils.throwIf(!requesterUid.equals(uid), ErrorCode.NO_AUTH_ERROR);
        return ResultUtils.success(brokerageOrderService.getPayableOrder(orderId, uid));
    }

    @PostMapping("/payment/success")
    public BaseResponse<Boolean> onPaymentSuccess(@RequestBody PaymentSuccessEvent event) {
        requireUid();
        ThrowUtils.throwIf(event == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(event.getContextId() == null, ErrorCode.PARAMS_ERROR);

        String ctx = event.getContextType() == null ? "" : event.getContextType().trim().toUpperCase();
        if (!"BROKERAGE_ORDER".equals(ctx)) {
            return ResultUtils.success(false);
        }

        brokerageOrderService.onPaymentSuccess(event.getContextId(), event.getSuccessTime(), event.getChannel());
        return ResultUtils.success(true);
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_CONTACT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_CONTACT_LIMIT);
    }

    private static Long requireUid() {
        RequestInfo info = RequestHolder.get();
        if (info == null || info.getUid() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return info.getUid();
    }

    @lombok.Data
    public static class ConfirmWeeklyScheduleRequest {
        private String classTime;
        private Integer frequencyPerWeek;
        private Long lessonPriceFen;
    }

    @lombok.Data
    public static class TrialCanceledRequest {
        private String reason;
    }
}
