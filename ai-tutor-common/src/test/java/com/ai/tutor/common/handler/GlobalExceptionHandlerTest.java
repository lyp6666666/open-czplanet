package com.ai.tutor.common.handler;

import com.ai.tutor.common.BaseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void shouldReturnReadableMessageWhenUploadFileIsTooLarge() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        BaseResponse<?> response = handler.handleMaxUploadSizeExceededException(new MaxUploadSizeExceededException(1024));

        assertThat(response.getCode()).isEqualTo(40000);
        assertThat(response.getMessage()).isEqualTo("上传文件过大，请控制在 20MB 以内");
    }
}
