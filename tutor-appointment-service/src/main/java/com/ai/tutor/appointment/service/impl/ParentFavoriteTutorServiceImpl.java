package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.ParentFavoriteTutorMapper;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.entity.ParentFavoriteTutor;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.service.ParentFavoriteTutorService;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParentFavoriteTutorServiceImpl implements ParentFavoriteTutorService {

    @Resource
    private ParentFavoriteTutorMapper parentFavoriteTutorMapper;

    @Override
    public void favorite(Long parentId, Long tutorId) {
        ThrowUtils.throwIf(parentId == null || tutorId == null, ErrorCode.PARAMS_ERROR);
        Integer exists = parentFavoriteTutorMapper.exists(parentId, tutorId);
        if (exists != null && exists > 0) return;
        parentFavoriteTutorMapper.insert(parentId, tutorId);
    }

    @Override
    public void unfavorite(Long parentId, Long tutorId) {
        ThrowUtils.throwIf(parentId == null || tutorId == null, ErrorCode.PARAMS_ERROR);
        parentFavoriteTutorMapper.delete(parentId, tutorId);
    }

    @Override
    public List<Long> checkFavoritedTutorIds(Long parentId, List<Long> tutorIds) {
        ThrowUtils.throwIf(parentId == null, ErrorCode.PARAMS_ERROR);
        if (tutorIds == null || tutorIds.isEmpty()) return List.of();
        return parentFavoriteTutorMapper.listFavoritedTutorIds(parentId, tutorIds);
    }

    @Override
    public CursorPageResponse<Long> pageFavoritedTutorIds(Long parentId, CursorPageRequest request) {
        ThrowUtils.throwIf(parentId == null || request == null, ErrorCode.PARAMS_ERROR);
        Integer pageSize = request.getPageSize();
        List<ParentFavoriteTutor> rows = parentFavoriteTutorMapper.listByParent(parentId, request.getCursor(), pageSize);
        List<Long> list = rows == null ? List.of() : rows.stream().map(ParentFavoriteTutor::getTutorId).toList();

        Long nextCursor = null;
        if (rows != null && !rows.isEmpty()) {
            nextCursor = rows.get(rows.size() - 1).getId();
        }
        boolean isLast = rows == null || rows.size() < pageSize;
        return new CursorPageResponse<>(nextCursor, isLast, list);
    }
}

