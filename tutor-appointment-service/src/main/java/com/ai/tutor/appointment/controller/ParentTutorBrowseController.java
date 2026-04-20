package com.ai.tutor.appointment.controller;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.parent.ParentTutorVOs;
import com.ai.tutor.appointment.service.ParentTutorBrowseService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.RequestHolder;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/parent/tutors")
@Tag(name = "学生找老师接口", description = "学生端教师列表/搜索（游标分页）")
public class ParentTutorBrowseController {

    @Resource
    private ParentTutorBrowseService parentTutorBrowseService;

    @GetMapping("/page")
    @Operation(summary = "教师列表（关键词/筛选/游标分页）")
    public BaseResponse<CursorPageResponse<ParentTutorVOs.TutorCardVO>> page(@RequestParam(value = "q", required = false) String q,
                                                                            @RequestParam(value = "city", required = false) String city,
                                                                            @RequestParam(value = "subject", required = false) String subject,
                                                                            @RequestParam(value = "rateMin", required = false) BigDecimal rateMin,
                                                                            @RequestParam(value = "rateMax", required = false) BigDecimal rateMax,
                                                                            @Valid CursorPageRequest pageRequest) {
        CursorPageResponse<ParentTutorVOs.TutorCardVO> page = parentTutorBrowseService.pageTutors(
                RequestHolder.get().getUid(),
                q,
                city,
                subject,
                rateMin,
                rateMax,
                pageRequest
        );
        return ResultUtils.success(page);
    }
}
