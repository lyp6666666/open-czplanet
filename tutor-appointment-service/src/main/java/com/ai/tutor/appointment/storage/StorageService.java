package com.ai.tutor.appointment.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 统一存储服务。
 * 当前以“图片上传+对外可访问 URL”作为核心能力，便于前端直接渲染。
 */
public interface StorageService {

    UploadResult uploadImage(AssetBiz biz, Long uid, MultipartFile file);
}

