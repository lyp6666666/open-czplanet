package com.ai.tutor.payment.controller;

import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.payment.controller.dto.PaymentOrderStatusResponse;
import com.ai.tutor.payment.controller.dto.PrepayResponse;
import com.ai.tutor.payment.service.YungouosPaymentAppService;
import com.ai.tutor.utils.RequestHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = YungouosPaymentController.class)
@Import(YungouosPaymentControllerTest.TestRequestHolderFilter.class)
public class YungouosPaymentControllerTest {

    @Component
    public static class TestRequestHolderFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
            RequestInfo info = new RequestInfo();
            info.setUid(1L);
            info.setIp("127.0.0.1");
            RequestHolder.set(info);
            try {
                filterChain.doFilter(request, response);
            } finally {
                RequestHolder.remove();
            }
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private YungouosPaymentAppService yungouosPaymentAppService;

    @Test
    void prepay_returnsData() throws Exception {
        PrepayResponse resp = new PrepayResponse();
        resp.setOrderNo("O1");
        resp.setAmountFen(100L);
        resp.setChannel("WECHAT");
        resp.setQrCodeUrl("http://img.example.com/q.png");
        when(yungouosPaymentAppService.prepay(any(), eq(1L), anyString())).thenReturn(resp);

        mockMvc.perform(post("/payment/prepay")
                        .contentType("application/json")
                        .content("{\"contextType\":\"BROKERAGE_ORDER\",\"contextId\":10,\"channel\":\"WECHAT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderNo").value("O1"))
                .andExpect(jsonPath("$.data.qrCodeUrl").value("http://img.example.com/q.png"));
    }

    @Test
    void orderStatus_returnsData() throws Exception {
        PaymentOrderStatusResponse resp = new PaymentOrderStatusResponse();
        resp.setOrderNo("O2");
        resp.setStatus("PENDING");
        resp.setAmountFen(100L);
        resp.setChannel("WECHAT");
        when(yungouosPaymentAppService.getOrderStatus(eq("O2"), eq(1L))).thenReturn(resp);

        mockMvc.perform(get("/payment/orders/O2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderNo").value("O2"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }
}
