package com.ai.tutor.appointment.storage;

import lombok.Getter;

/**
 * 资源上传业务类型。
 * 用于区分上传限制、对象 key 前缀等策略。
 */
@Getter
public enum AssetBiz {
    AVATAR("avatar"),
    BANNER("banner"),
    POST("post"),
    OTHER("other");

    private final String code;

    AssetBiz(String code) {
        this.code = code;
    }

    public static AssetBiz fromCode(String raw) {
        if (raw == null) {
            return OTHER;
        }
        String v = raw.trim().toLowerCase();
        for (AssetBiz b : values()) {
            if (b.code.equals(v)) {
                return b;
            }
        }
        return OTHER;
    }
}

