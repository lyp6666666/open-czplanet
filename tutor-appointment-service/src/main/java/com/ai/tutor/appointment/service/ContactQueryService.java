package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.vo.UserSimpleVO;

import java.util.List;

public interface ContactQueryService {

    List<UserSimpleVO> recentContacts(Long uid, Integer limit);

    List<UserSimpleVO> searchContacts(Long uid, Integer role, String keyword, Integer limit);
}
