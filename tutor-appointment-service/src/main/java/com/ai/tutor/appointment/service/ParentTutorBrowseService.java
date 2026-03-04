package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.parent.ParentTutorVOs;

import java.math.BigDecimal;

public interface ParentTutorBrowseService {
    CursorPageResponse<ParentTutorVOs.TutorCardVO> pageTutors(Long uid,
                                                             String q,
                                                             String city,
                                                             String mode,
                                                             String subject,
                                                             BigDecimal rateMin,
                                                             BigDecimal rateMax,
                                                             CursorPageRequest pageRequest);
}
