package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.vo.SubjectTreeNodeVO;
import com.ai.tutor.appointment.service.SubjectQueryService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
@Tag(name = "科目模块接口", description = "提供科目树与科目搜索接口")
public class SubjectController {

    @Resource
    private SubjectQueryService subjectQueryService;

    @GetMapping("/tree")
    @Operation(summary = "获取科目树", description = "返回启用的科目树结构（两级或多级）")
    public BaseResponse<List<SubjectTreeNodeVO>> tree() {
        return ResultUtils.success(subjectQueryService.getEnabledTree());
    }

    @GetMapping("/search")
    @Operation(summary = "搜索科目", description = "根据关键词模糊匹配科目名称，返回扁平列表")
    public BaseResponse<List<SubjectTreeNodeVO>> search(@RequestParam("keyword") String keyword,
                                                       @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        return ResultUtils.success(subjectQueryService.searchEnabledByKeyword(keyword, limit));
    }
}
