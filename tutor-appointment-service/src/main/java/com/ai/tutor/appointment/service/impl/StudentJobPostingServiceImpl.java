package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.StudentJobPostingMapper;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.job.CreateStudentJobPostingRequest;
import com.ai.tutor.appointment.model.dto.job.UpdateStudentJobPostingRequest;
import com.ai.tutor.appointment.model.entity.StudentJobPosting;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.service.StudentJobPostingService;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StudentJobPostingServiceImpl implements StudentJobPostingService {

    @Resource
    private StudentJobPostingMapper studentJobPostingMapper;

    @Override
    public Long create(CreateStudentJobPostingRequest request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);

        StudentJobPosting posting = StudentJobPosting.builder()
                .parentId(uid)
                .subjectId(request.getSubjectId())
                .title(request.getTitle())
                .description(request.getDescription())
                .childAge(request.getChildAge())
                .classMode(request.getClassMode())
                .city(request.getCity())
                .address(request.getAddress())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .stageCode(request.getStageCode())
                .educationRequirement(normalizeEducationRequirement(request.getEducationRequirement()))
                .schedule(request.getSchedule())
                .status(1)
                .build();
        int inserted = studentJobPostingMapper.insert(posting);
        ThrowUtils.throwIf(inserted <= 0 || posting.getId() == null, ErrorCode.OPERATION_ERROR);
        return posting.getId();
    }

    @Override
    public void update(Long id, UpdateStudentJobPostingRequest request, Long uid) {
        ThrowUtils.throwIf(id == null || request == null || uid == null, ErrorCode.PARAMS_ERROR);

        StudentJobPosting db = studentJobPostingMapper.selectById(id);
        ThrowUtils.throwIf(db == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(db.getParentId()), ErrorCode.NO_AUTH_ERROR);

        StudentJobPosting toUpdate = StudentJobPosting.builder()
                .id(id)
                .subjectId(request.getSubjectId())
                .title(request.getTitle())
                .description(request.getDescription())
                .childAge(request.getChildAge())
                .classMode(request.getClassMode())
                .city(request.getCity())
                .address(request.getAddress())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .stageCode(request.getStageCode())
                .educationRequirement(normalizeEducationRequirement(request.getEducationRequirement()))
                .schedule(request.getSchedule())
                .status(request.getStatus())
                .build();
        int updated = studentJobPostingMapper.updateById(toUpdate);
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public StudentJobPosting getById(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        StudentJobPosting posting = studentJobPostingMapper.selectById(id);
        ThrowUtils.throwIf(posting == null, ErrorCode.NOT_FOUND_ERROR);
        return posting;
    }

    @Override
    public CursorPageResponse<StudentJobPosting> listMine(CursorPageRequest request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);
        Integer pageSize = request.getPageSize();

        List<StudentJobPosting> list = studentJobPostingMapper.listByParentId(uid, request.getCursor(), pageSize);
        return buildCursorResponse(list, pageSize);
    }

    @Override
    public CursorPageResponse<StudentJobPosting> listPublished(Long subjectId,
                                                              String city,
                                                              String classMode,
                                                              String stageCode,
                                                              String educationRequirement,
                                                              BigDecimal budgetMin,
                                                              BigDecimal budgetMax,
                                                              String keyword,
                                                              String sort,
                                                              CursorPageRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Integer pageSize = request.getPageSize();

        String edu = normalizeEducationRequirement(educationRequirement);
        List<StudentJobPosting> list = studentJobPostingMapper.listPublishedFiltered(
                subjectId,
                city,
                classMode,
                stageCode,
                edu,
                budgetMin,
                budgetMax,
                keyword,
                sort,
                request.getCursor(),
                pageSize
        );
        return buildCursorResponse(list, pageSize);
    }

    private static String normalizeEducationRequirement(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        if ("UNLIMITED".equalsIgnoreCase(v)) return null;
        return v;
    }

    private static CursorPageResponse<StudentJobPosting> buildCursorResponse(List<StudentJobPosting> list, Integer pageSize) {
        Long nextCursor = null;
        if (list != null && !list.isEmpty()) {
            nextCursor = list.get(list.size() - 1).getId();
        }
        boolean isLast = list == null || list.size() < pageSize;
        return new CursorPageResponse<>(nextCursor, isLast, list);
    }
}
