package com.ai.tutor.utils;


import com.ai.tutor.common.service.dto.RequestInfo;

/**
 * 请求上下文
 */
public class RequestHolder {

    public static final String ATTRIBUTE_UID = "uid";
    public static final String ATTRIBUTE_PHONE = "phone";

    private static final ThreadLocal<RequestInfo> threadLocal = new ThreadLocal<>();

    public static void set(RequestInfo requestInfo) {
        threadLocal.set(requestInfo);
    }

    public static RequestInfo get() {
        return threadLocal.get();
    }

    public static void remove() {
        threadLocal.remove();
    }
}
