package com.ai.tutor.videocallimservice.integration;

import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.videocallimservice.common.domain.entity.ImUser;
import com.ai.tutor.videocallimservice.integration.feign.AppointmentInternalFeignClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class AppointmentInternalClientTest {

    @Mock
    private AppointmentInternalFeignClient client;

    @InjectMocks
    private AppointmentInternalClient internalClient;

    @Test
    void shouldDelegateGetUserBasicToFeign() {
        Map<String, Object> basic = new HashMap<>();
        basic.put("id", 206L);
        basic.put("userType", 1);
        basic.put("refId", 1001L);
        basic.put("status", 0);
        when(client.getUserBasicById(206L)).thenReturn(new BaseResponse<>(0, basic, "ok"));

        ImUser out = internalClient.getUserBasicById(206L);

        assertThat(out).isNotNull();
        assertThat(out.getId()).isEqualTo(206L);
        assertThat(out.getUserType()).isEqualTo(1);
        assertThat(out.getRefId()).isEqualTo(1001L);
        assertThat(out.getStatus()).isEqualTo(0);
        verify(client).getUserBasicById(206L);
    }

    @Test
    void shouldThrowWhenFeignReturnsBusinessError() {
        when(client.getUserBasicById(206L)).thenReturn(new BaseResponse<>(40400, null, "请求数据不存在"));

        assertThatThrownBy(() -> internalClient.getUserBasicById(206L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请求数据不存在");
    }
}
