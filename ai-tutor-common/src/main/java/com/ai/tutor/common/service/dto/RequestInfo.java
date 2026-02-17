package com.ai.tutor.common.service.dto;

import lombok.Data;

/**
 * Description: web请求信息收集类
 */
@Data
public class RequestInfo {
    private Long uid;
    private String ip;
    private Integer role;
}
