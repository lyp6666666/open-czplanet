package com.ai.tutor.appointment.integration;

import com.ai.tutor.appointment.integration.feign.ImInternalFeignClient;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.utils.RequestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HttpImFacadeTest {

    @Mock
    private ImInternalFeignClient client;

    @InjectMocks
    private HttpImFacade facade;

    @AfterEach
    void tearDown() {
        RequestHolder.remove();
    }

    @Test
    void shouldCallFeignClientGetOrCreateRoom() {
        bindUid(206L);
        when(client.getOrCreateRoomWithUser(any())).thenReturn(new BaseResponse<>(0, 1001L, "ok"));

        Long roomId = facade.getOrCreateRoomWithUser(206L, 113L);

        assertThat(roomId).isEqualTo(1001L);
        ArgumentCaptor<ImInternalFeignClient.ImRoomRequest> captor = ArgumentCaptor.forClass(ImInternalFeignClient.ImRoomRequest.class);
        verify(client).getOrCreateRoomWithUser(captor.capture());
        assertThat(captor.getValue().getTargetUid()).isEqualTo(113L);
    }

    @Test
    void shouldCallFeignClientSendSystemMessage() {
        bindUid(206L);
        when(client.sendSystemMessage(any())).thenReturn(new BaseResponse<>(0, 9001L, "ok"));

        Long msgId = facade.sendSystemMessage(206L, 88L, Map.of("bizType", "LESSON_REQUEST"));

        assertThat(msgId).isEqualTo(9001L);
        ArgumentCaptor<ImInternalFeignClient.ImSystemMessageRequest> captor =
                ArgumentCaptor.forClass(ImInternalFeignClient.ImSystemMessageRequest.class);
        verify(client).sendSystemMessage(captor.capture());
        assertThat(captor.getValue().getRoomId()).isEqualTo(88L);
        assertThat(captor.getValue().getBody()).isEqualTo(Map.of("bizType", "LESSON_REQUEST"));
    }

    @Test
    void shouldCallFeignClientAssertRoomReadyForScheduling() {
        bindUid(206L);
        when(client.assertRoomReadyForScheduling(88L)).thenReturn(new BaseResponse<>(0, true, "ok"));

        facade.assertRoomReadyForScheduling(206L, 88L);

        verify(client).assertRoomReadyForScheduling(88L);
    }

    @Test
    void shouldCallFeignClientConfirmWeeklyScheduleSubmitted() {
        bindUid(206L);
        when(client.confirmWeeklyScheduleSubmitted(any(), any())).thenReturn(new BaseResponse<>(0, true, "ok"));

        facade.confirmWeeklyScheduleSubmitted(206L, 66L, "周二 19:00-21:00", 1, 20000L);

        ArgumentCaptor<ImInternalFeignClient.ConfirmWeeklyScheduleRequest> captor =
                ArgumentCaptor.forClass(ImInternalFeignClient.ConfirmWeeklyScheduleRequest.class);
        verify(client).confirmWeeklyScheduleSubmitted(org.mockito.ArgumentMatchers.eq(66L), captor.capture());
        assertThat(captor.getValue().getClassTime()).isEqualTo("周二 19:00-21:00");
        assertThat(captor.getValue().getFrequencyPerWeek()).isEqualTo(1);
        assertThat(captor.getValue().getLessonPriceFen()).isEqualTo(20000L);
    }

    @Test
    void shouldCallFeignClientMarkTrialCanceled() {
        bindUid(206L);
        when(client.markTrialCanceled(any(), any())).thenReturn(new BaseResponse<>(0, true, "ok"));

        facade.markTrialCanceled(206L, 66L, "临时取消试课");

        ArgumentCaptor<ImInternalFeignClient.TrialCanceledRequest> captor =
                ArgumentCaptor.forClass(ImInternalFeignClient.TrialCanceledRequest.class);
        verify(client).markTrialCanceled(org.mockito.ArgumentMatchers.eq(66L), captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("临时取消试课");
    }

    @Test
    void shouldThrowWhenFeignReturnsBusinessError() {
        bindUid(206L);
        when(client.listRecentContactUids(20)).thenReturn(new BaseResponse<>(50000, null, "im error"));

        assertThatThrownBy(() -> facade.listRecentContactUids(206L, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessage("im error");
    }

    @Test
    void shouldThrowWhenUidDoesNotMatchRequestContext() {
        bindUid(300L);

        assertThatThrownBy(() -> facade.getOrCreateRoomWithUser(206L, 113L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldReturnEmptyListWhenRemoteDataIsNull() {
        bindUid(206L);
        when(client.listRecentContactUids(20)).thenReturn(new BaseResponse<>(0, null, "ok"));

        List<Long> uids = facade.listRecentContactUids(206L, 0);

        assertThat(uids).isEmpty();
    }

    private static void bindUid(Long uid) {
        RequestInfo info = new RequestInfo();
        info.setUid(uid);
        info.setRole(1);
        RequestHolder.set(info);
    }
}
