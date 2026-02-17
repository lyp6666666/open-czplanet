package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.service.TutorFavoriteDemandService;
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
@RequestMapping("/api/v1/tutor/favorites/demands")
@Tag(name = "教师收藏需求接口", description = "教师收藏/取消收藏家长需求贴，并查询收藏状态")
public class TutorFavoriteDemandController {

    @Resource
    private TutorFavoriteDemandService tutorFavoriteDemandService;

    @PostMapping("/{demandId}")
    @Operation(summary = "收藏需求贴")
    public BaseResponse<String> favorite(@PathVariable("demandId") Long demandId) {
        tutorFavoriteDemandService.favorite(RequestHolder.get().getUid(), demandId);
        return ResultUtils.success("OK");
    }

    @DeleteMapping("/{demandId}")
    @Operation(summary = "取消收藏需求贴")
    public BaseResponse<String> unfavorite(@PathVariable("demandId") Long demandId) {
        tutorFavoriteDemandService.unfavorite(RequestHolder.get().getUid(), demandId);
        return ResultUtils.success("OK");
    }

    @GetMapping("/check")
    @Operation(summary = "批量查询收藏的需求ID")
    public BaseResponse<List<Long>> check(@RequestParam(value = "ids", required = false) List<Long> ids) {
        List<Long> list = tutorFavoriteDemandService.checkFavoritedDemandIds(RequestHolder.get().getUid(), ids);
        return ResultUtils.success(list);
    }

    @GetMapping("/page")
    @Operation(summary = "收藏需求ID列表（游标分页）")
    public BaseResponse<CursorPageResponse<Long>> page(@Valid CursorPageRequest request) {
        CursorPageResponse<Long> page = tutorFavoriteDemandService.pageFavoritedDemandIds(RequestHolder.get().getUid(), request);
        return ResultUtils.success(page);
    }
}

