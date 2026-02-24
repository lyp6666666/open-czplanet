package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;

import java.util.List;

public interface ParentFavoriteTutorService {

    void favorite(Long parentId, Long tutorId);

    void unfavorite(Long parentId, Long tutorId);

    List<Long> checkFavoritedTutorIds(Long parentId, List<Long> tutorIds);

    CursorPageResponse<Long> pageFavoritedTutorIds(Long parentId, CursorPageRequest request);
}

