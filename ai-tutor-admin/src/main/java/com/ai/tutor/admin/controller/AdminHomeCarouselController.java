package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.model.vo.AdminHomeCarouselItemVO;
import com.ai.tutor.admin.service.AdminHomeCarouselService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/home/carousel")
@Tag(name = "Admin Home Carousel", description = "首页轮播图运营配置")
public class AdminHomeCarouselController {

    @Resource
    private AdminHomeCarouselService adminHomeCarouselService;

    @GetMapping
    @Operation(summary = "获取首页轮播图列表")
    public BaseResponse<List<AdminHomeCarouselItemVO>> list() {
        return ResultUtils.success(adminHomeCarouselService.list());
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "上传首页轮播图")
    public BaseResponse<AdminHomeCarouselItemVO> create(@RequestParam("file") MultipartFile file,
                                                        @RequestParam("title") String title,
                                                        @RequestParam(value = "subtitle", required = false) String subtitle,
                                                        @RequestParam(value = "linkUrl", required = false) String linkUrl) {
        Long adminUid = RequestHolder.get() == null ? null : RequestHolder.get().getUid();
        return ResultUtils.success(adminHomeCarouselService.create(title, subtitle, linkUrl, file, adminUid));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除首页轮播图")
    public BaseResponse<Boolean> delete(@PathVariable("id") Long id) {
        Long adminUid = RequestHolder.get() == null ? null : RequestHolder.get().getUid();
        adminHomeCarouselService.delete(id, adminUid);
        return ResultUtils.success(true);
    }
}
