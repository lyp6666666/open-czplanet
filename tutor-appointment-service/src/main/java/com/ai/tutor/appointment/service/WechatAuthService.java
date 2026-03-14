package com.ai.tutor.appointment.service;

import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;

public interface WechatAuthService {
    WxMaJscode2SessionResult login(String code);
}
