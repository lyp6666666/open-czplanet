package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.config.WechatProperties;
import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.ai.tutor.appointment.service.WechatAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import me.chanjar.weixin.common.error.WxErrorException;

@Service
@Slf4j
@RequiredArgsConstructor
public class WechatAuthServiceImpl implements WechatAuthService {

    private final WxMaService wxMaService;
    private final WechatProperties wechatProperties;

    @Override
    public WxMaJscode2SessionResult login(String code) {
        if (Boolean.TRUE.equals(wechatProperties.getMockEnabled())) {
            log.info("Mock Wechat Login with code: {}", code);
            WxMaJscode2SessionResult mock = new WxMaJscode2SessionResult();
            mock.setOpenid("mock_openid_" + code);
            mock.setSessionKey("mock_session_key");
            return mock;
        }
        try {
            return wxMaService.getUserService().getSessionInfo(code);
        } catch (WxErrorException e) {
            log.error("Wechat login failed", e);
            throw new RuntimeException("Wechat login failed: " + e.getMessage());
        }
    }
}
