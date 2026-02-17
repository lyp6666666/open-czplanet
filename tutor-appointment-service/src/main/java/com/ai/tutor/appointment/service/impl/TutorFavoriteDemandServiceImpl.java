package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.TutorFavoriteDemandMapper;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.entity.TutorFavoriteDemand;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.service.TutorFavoriteDemandService;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TutorFavoriteDemandServiceImpl implements TutorFavoriteDemandService {

    @Resource
    private TutorFavoriteDemandMapper tutorFavoriteDemandMapper;

    @Override
    public void favorite(Long tutorId, Long demandId) {
        ThrowUtils.throwIf(tutorId == null || demandId == null, ErrorCode.PARAMS_ERROR);
        Integer exists = tutorFavoriteDemandMapper.exists(tutorId, demandId);
        if (exists != null && exists > 0) return;
        tutorFavoriteDemandMapper.insert(tutorId, demandId);
    }

    @Override
    public void unfavorite(Long tutorId, Long demandId) {
        ThrowUtils.throwIf(tutorId == null || demandId == null, ErrorCode.PARAMS_ERROR);
        tutorFavoriteDemandMapper.delete(tutorId, demandId);
    }

    @Override
    public List<Long> checkFavoritedDemandIds(Long tutorId, List<Long> demandIds) {
        ThrowUtils.throwIf(tutorId == null, ErrorCode.PARAMS_ERROR);
        if (demandIds == null || demandIds.isEmpty()) return List.of();
        return tutorFavoriteDemandMapper.listFavoritedDemandIds(tutorId, demandIds);
    }

    @Override
    public CursorPageResponse<Long> pageFavoritedDemandIds(Long tutorId, CursorPageRequest request) {
        ThrowUtils.throwIf(tutorId == null || request == null, ErrorCode.PARAMS_ERROR);
        Integer pageSize = request.getPageSize();
        List<TutorFavoriteDemand> rows = tutorFavoriteDemandMapper.listByTutor(tutorId, request.getCursor(), pageSize);
        List<Long> list = rows == null ? List.of() : rows.stream().map(TutorFavoriteDemand::getDemandId).toList();

        Long nextCursor = null;
        if (rows != null && !rows.isEmpty()) {
            nextCursor = rows.get(rows.size() - 1).getId();
        }
        boolean isLast = rows == null || rows.size() < pageSize;
        return new CursorPageResponse<>(nextCursor, isLast, list);
    }
}
