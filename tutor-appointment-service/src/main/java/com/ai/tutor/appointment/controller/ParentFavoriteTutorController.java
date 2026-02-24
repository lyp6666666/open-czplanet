package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.service.ParentFavoriteTutorService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parent/favorites/tutors")
@Tag(name = "学生收藏教师接口", description = "学生收藏/取消收藏教师，并查询收藏状态")
public class ParentFavoriteTutorController {

    @Resource
    private ParentFavoriteTutorService parentFavoriteTutorService;

    @PostMapping("/{tutorId}")
    @Operation(summary = "收藏教师")
    public BaseResponse<String> favorite(@PathVariable("tutorId") Long tutorId) {
        parentFavoriteTutorService.favorite(RequestHolder.get().getUid(), tutorId);
        return ResultUtils.success("OK");
    }

    @DeleteMapping("/{tutorId}")
    @Operation(summary = "取消收藏教师")
    public BaseResponse<String> unfavorite(@PathVariable("tutorId") Long tutorId) {
        parentFavoriteTutorService.unfavorite(RequestHolder.get().getUid(), tutorId);
        return ResultUtils.success("OK");
    }

    @GetMapping("/check")
    @Operation(summary = "批量查询收藏的教师ID")
    public BaseResponse<List<Long>> check(@RequestParam(value = "ids", required = false) List<Long> ids) {
        List<Long> list = parentFavoriteTutorService.checkFavoritedTutorIds(RequestHolder.get().getUid(), ids);
        return ResultUtils.success(list);
    }

    @GetMapping("/page")
    @Operation(summary = "收藏教师ID列表（游标分页）")
    public BaseResponse<CursorPageResponse<Long>> page(@Valid CursorPageRequest request) {
        CursorPageResponse<Long> page = parentFavoriteTutorService.pageFavoritedTutorIds(RequestHolder.get().getUid(), request);
        return ResultUtils.success(page);
    }
}

