package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.TeacherJobPostingMapper;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.job.CreateTeacherJobPostingRequest;
import com.ai.tutor.appointment.model.dto.job.UpdateTeacherJobPostingRequest;
import com.ai.tutor.appointment.model.entity.TeacherJobPosting;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.service.TeacherJobPostingService;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeacherJobPostingServiceImpl implements TeacherJobPostingService {

    @Resource
    private TeacherJobPostingMapper teacherJobPostingMapper;

    @Override
    public Long create(CreateTeacherJobPostingRequest request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);

        TeacherJobPosting posting = TeacherJobPosting.builder()
                .tutorId(uid)
                .subjectId(request.getSubjectId())
                .title(request.getTitle())
                .description(request.getDescription())
                .pricePerHour(request.getPricePerHour())
                .mode(request.getMode())
                .city(request.getCity())
                .availableTime(request.getAvailableTime())
                .maxStudents(request.getMaxStudents())
                .status(1)
                .build();

        int inserted = teacherJobPostingMapper.insert(posting);
        ThrowUtils.throwIf(inserted <= 0 || posting.getId() == null, ErrorCode.OPERATION_ERROR);
        return posting.getId();
    }

    @Override
    public void update(Long id, UpdateTeacherJobPostingRequest request, Long uid) {
        ThrowUtils.throwIf(id == null || request == null || uid == null, ErrorCode.PARAMS_ERROR);

        TeacherJobPosting db = teacherJobPostingMapper.selectById(id);
        ThrowUtils.throwIf(db == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(db.getTutorId()), ErrorCode.NO_AUTH_ERROR);

        TeacherJobPosting toUpdate = TeacherJobPosting.builder()
                .id(id)
                .subjectId(request.getSubjectId())
                .title(request.getTitle())
                .description(request.getDescription())
                .pricePerHour(request.getPricePerHour())
                .mode(request.getMode())
                .city(request.getCity())
                .availableTime(request.getAvailableTime())
                .maxStudents(request.getMaxStudents())
                .status(request.getStatus())
                .build();
        int updated = teacherJobPostingMapper.updateById(toUpdate);
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public TeacherJobPosting getById(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        TeacherJobPosting posting = teacherJobPostingMapper.selectById(id);
        ThrowUtils.throwIf(posting == null, ErrorCode.NOT_FOUND_ERROR);
        return posting;
    }

    @Override
    public CursorPageResponse<TeacherJobPosting> listMine(CursorPageRequest request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);
        Integer pageSize = request.getPageSize();

        List<TeacherJobPosting> list = teacherJobPostingMapper.listByTutorId(uid, request.getCursor(), pageSize);
        return buildCursorResponse(list, pageSize);
    }

    @Override
    public CursorPageResponse<TeacherJobPosting> listPublished(Long subjectId, String city, String mode, CursorPageRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Integer pageSize = request.getPageSize();

        List<TeacherJobPosting> list = teacherJobPostingMapper.listPublished(subjectId, city, mode, request.getCursor(), pageSize);
        return buildCursorResponse(list, pageSize);
    }

    private static CursorPageResponse<TeacherJobPosting> buildCursorResponse(List<TeacherJobPosting> list, Integer pageSize) {
        Long nextCursor = null;
        if (list != null && !list.isEmpty()) {
            nextCursor = list.get(list.size() - 1).getId();
        }
        boolean isLast = list == null || list.size() < pageSize;
        return new CursorPageResponse<>(nextCursor, isLast, list);
    }
}
