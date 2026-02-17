package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;

import java.util.List;

public interface TutorFavoriteDemandService {

    void favorite(Long tutorId, Long demandId);

    void unfavorite(Long tutorId, Long demandId);

    List<Long> checkFavoritedDemandIds(Long tutorId, List<Long> demandIds);

    CursorPageResponse<Long> pageFavoritedDemandIds(Long tutorId, CursorPageRequest request);
}

