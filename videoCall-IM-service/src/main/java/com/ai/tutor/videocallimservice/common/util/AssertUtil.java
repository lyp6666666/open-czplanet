package com.ai.tutor.videocallimservice.common.util;

import cn.hutool.core.util.ObjectUtil;
import com.ai.tutor.exception.BusinessErrorEnum;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.exception.ErrorEnum;

import java.text.MessageFormat;
import java.util.Objects;

public class AssertUtil {

    //如果不是非空对象，则抛异常
    public static void isNotEmpty(Object obj, String msg) {
        if (isEmpty(obj)) {
            throwException(msg);
        }
    }


    private static void throwException(String msg) {
        throwException(null, msg);
    }

    private static void throwException(ErrorEnum errorEnum, Object... arg) {
        if (Objects.isNull(errorEnum)) {
            errorEnum = BusinessErrorEnum.BUSINESS_ERROR;
        }
        throw new BusinessException(errorEnum.getErrorCode(), MessageFormat.format(errorEnum.getErrorMsg(), arg));
    }

    private static boolean isEmpty(Object obj) {
        return ObjectUtil.isEmpty(obj);
    }
}
