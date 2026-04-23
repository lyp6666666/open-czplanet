package com.ai.tutor.liveclass.service;

import com.ai.tutor.liveclass.config.LiveKitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class LiveKitUrlResolverTest {

    @Test
    void shouldNotExposeLocalhostFromForwardedHostToBrowser() {
        LiveKitProperties properties = new LiveKitProperties();
        properties.setWsUrl("ws://127.0.0.1:7880");

        LiveKitUrlResolver resolver = new LiveKitUrlResolver();
        ReflectionTestUtils.setField(resolver, "liveKitProperties", properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "localhost:18080");
        request.addHeader("X-Forwarded-Host", "localhost:18080");
        request.addHeader("X-Forwarded-Proto", "http");

        assertThat(resolver.resolvePublicWsUrl(request)).isEqualTo("wss://huoyue.online/livekit");
    }

    @Test
    void shouldNotExposeRawIpWsUrlToHttpsBrowser() {
        LiveKitProperties properties = new LiveKitProperties();
        properties.setWsUrl("ws://111.228.20.88/livekit");

        LiveKitUrlResolver resolver = new LiveKitUrlResolver();
        ReflectionTestUtils.setField(resolver, "liveKitProperties", properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        request.addHeader("Host", "111.228.20.88");
        request.addHeader("X-Forwarded-Host", "111.228.20.88");
        request.addHeader("X-Forwarded-Proto", "https");

        assertThat(resolver.resolvePublicWsUrl(request)).isEqualTo("wss://huoyue.online/livekit");
    }

    @Test
    void shouldPreferConfiguredPublicWsUrl() {
        LiveKitProperties properties = new LiveKitProperties();
        properties.setWsUrl("ws://127.0.0.1:7880");
        properties.setPublicWsUrl("wss://class.example.com/livekit/");

        LiveKitUrlResolver resolver = new LiveKitUrlResolver();
        ReflectionTestUtils.setField(resolver, "liveKitProperties", properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "localhost:18080");

        assertThat(resolver.resolvePublicWsUrl(request)).isEqualTo("wss://class.example.com/livekit");
    }
}
