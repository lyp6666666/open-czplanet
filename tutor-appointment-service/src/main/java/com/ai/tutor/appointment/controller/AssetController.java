package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.storage.AssetBiz;
import com.ai.tutor.appointment.storage.StorageService;
import com.ai.tutor.appointment.storage.UploadResult;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import com.ai.tutor.utils.ThrowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 统一资源上传入口。
 * 约定：上传后返回可直接访问的 url，前端无需拼接域名即可渲染。
 */
@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Assets", description = "统一资源服务")
public class AssetController {

    @Resource
    private StorageService storageService;

    @PostMapping("/upload")
    @Operation(summary = "上传图片资源（MinIO）")
    public BaseResponse<UploadResult> upload(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "biz", required = false) String biz) {
        Long uid = RequestHolder.get() == null ? null : RequestHolder.get().getUid();
        ThrowUtils.throwIf(uid == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        AssetBiz b = AssetBiz.fromCode(biz);
        return ResultUtils.success(storageService.uploadImage(b, uid, file));
    }
}

