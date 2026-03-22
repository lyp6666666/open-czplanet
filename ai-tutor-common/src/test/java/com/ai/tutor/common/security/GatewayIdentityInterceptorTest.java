package com.ai.tutor.common.security;

import com.ai.tutor.common.service.dto.RequestInfo;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.utils.RequestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayIdentityInterceptorTest {
    private static final String STRONG_SECRET = "0123456789abcdef0123456789abcdef";

    private IdentitySignProperties properties;
    private IdentitySignatureUtils signatureUtils;
    private GatewayIdentityInterceptor interceptor;

    @BeforeEach
    void setUp() {
        properties = new IdentitySignProperties();
        properties.setSecret(STRONG_SECRET);
        properties.setClockSkewMs(300_000L);
        signatureUtils = new IdentitySignatureUtils(properties);
        interceptor = new GatewayIdentityInterceptor(properties, signatureUtils);
    }

    @AfterEach
    void tearDown() {
        RequestHolder.remove();
    }

    @Test
    void shouldRejectWhenSignatureMismatch() {
        long ts = System.currentTimeMillis();
        MockHttpServletRequest req = signedRequest(ts, "bad-sign");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        assertThrows(BusinessException.class, () -> interceptor.preHandle(req, resp, new Object()));
    }

    @Test
    void shouldPopulateRequestHolderWhenSignatureValid() throws Exception {
        long ts = System.currentTimeMillis();
        String sign = signatureUtils.sign(206L, 1, ts, "POST", "/api/v1/test");
        MockHttpServletRequest req = signedRequest(ts, sign);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(req, resp, new Object()));

        RequestInfo info = RequestHolder.get();
        assertNotNull(info);
        assertEquals(206L, info.getUid());
        assertEquals(1, info.getRole());
        assertEquals("10.0.0.8", info.getIp());
        assertEquals("206", req.getAttribute(RequestHolder.ATTRIBUTE_UID));
        assertEquals(1, req.getAttribute(RequestHolder.ATTRIBUTE_ROLE));
    }

    private MockHttpServletRequest signedRequest(long ts, String sign) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/test");
        request.addHeader("X-Uid", "206");
        request.addHeader("X-Role", "1");
        request.addHeader("X-Ts", String.valueOf(ts));
        request.addHeader("X-Auth-Sign", sign);
        request.setRemoteAddr("10.0.0.8");
        return request;
    }
}
