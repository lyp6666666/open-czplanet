package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.SubjectTreeNodeVO;
import com.ai.tutor.appointment.model.vo.home.HomeGuestVOs;
import com.ai.tutor.appointment.service.HomeGuestService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "未登录首页接口", description = "未登录首页（Guest Home）数据聚合接口")
public class HomeGuestController {

    @Resource
    private HomeGuestService homeGuestService;

    @GetMapping("/home/config")
    @Operation(summary = "首页配置")
    public BaseResponse<HomeGuestVOs.HomeConfigVO> config(@RequestParam(value = "city", required = false) String city) {
        return ResultUtils.success(homeGuestService.getHomeConfig(city));
    }

    @GetMapping("/geo/locate")
    @Operation(summary = "IP 定位（简化版）")
    public BaseResponse<HomeGuestVOs.GeoLocateVO> locate(HttpServletRequest request) {
        return ResultUtils.success(homeGuestService.locate(request));
    }

    @GetMapping("/home/hot-words")
    @Operation(summary = "热门搜索词")
    public BaseResponse<HomeGuestVOs.HotWordsVO> hotWords(@RequestParam(value = "city", required = false) String city,
                                                         @RequestParam(value = "limit", required = false) Integer limit) {
        return ResultUtils.success(homeGuestService.getHotWords(city, limit));
    }

    @GetMapping("/home/search/suggest")
    @Operation(summary = "搜索联想")
    public BaseResponse<HomeGuestVOs.SearchSuggestVO> suggest(@RequestParam("q") String q,
                                                             @RequestParam(value = "city", required = false) String city,
                                                             @RequestParam(value = "limit", required = false) Integer limit) {
        return ResultUtils.success(homeGuestService.suggest(q, city, limit));
    }

    @GetMapping("/subjects/tree")
    @Operation(summary = "科目树")
    public BaseResponse<List<SubjectTreeNodeVO>> subjectTree() {
        return ResultUtils.success(homeGuestService.getSubjectTree());
    }

    @GetMapping("/home/banners")
    @Operation(summary = "首页 Banner 区")
    public BaseResponse<HomeGuestVOs.BannersVO> banners(@RequestParam(value = "city", required = false) String city,
                                                       @RequestParam(value = "scene", required = false) String scene) {
        return ResultUtils.success(homeGuestService.getBanners(city, scene));
    }

    @GetMapping("/home/hot-tabs")
    @Operation(summary = "热门 Tab")
    public BaseResponse<HomeGuestVOs.HotTabsVO> hotTabs(@RequestParam(value = "type", required = false) String type,
                                                       @RequestParam(value = "city", required = false) String city,
                                                       @RequestParam(value = "limit", required = false) Integer limit) {
        return ResultUtils.success(homeGuestService.getHotTabs(type, city, limit));
    }

    @PostMapping("/home/hot/services")
    @Operation(summary = "热门服务列表（游标分页）")
    public BaseResponse<CursorPageResponse<HomeGuestVOs.HotServiceCardVO>> hotServices(
            @RequestParam(value = "tabId", required = false) String tabId,
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "mode", required = false) String mode,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestBody(required = false) @Valid CursorPageRequest pageRequest
    ) {
        return ResultUtils.success(homeGuestService.getHotServices(tabId, subjectId, city, mode, sort, pageRequest));
    }

    @PostMapping("/home/hot/demands")
    @Operation(summary = "热门需求列表（游标分页）")
    public BaseResponse<CursorPageResponse<HomeGuestVOs.HotDemandCardVO>> hotDemands(
            @RequestParam(value = "tabId", required = false) String tabId,
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "classMode", required = false) String classMode,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestBody(required = false) @Valid CursorPageRequest pageRequest
    ) {
        return ResultUtils.success(homeGuestService.getHotDemands(tabId, subjectId, city, classMode, sort, pageRequest));
    }

    @PostMapping("/home/hot/tutors")
    @Operation(summary = "热门老师列表（游标分页）")
    public BaseResponse<CursorPageResponse<HomeGuestVOs.HotTutorCardVO>> hotTutors(
            @RequestParam(value = "subjectId", required = false) Long subjectId,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "mode", required = false) String mode,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestBody(required = false) @Valid CursorPageRequest pageRequest
    ) {
        return ResultUtils.success(homeGuestService.getHotTutors(subjectId, city, mode, sort, pageRequest));
    }

    @GetMapping("/home/footer-links")
    @Operation(summary = "页脚链接")
    public BaseResponse<HomeGuestVOs.FooterLinksVO> footerLinks() {
        return ResultUtils.success(homeGuestService.getFooterLinks());
    }
}

