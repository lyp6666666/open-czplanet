package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.vo.UserCardVO;
import com.ai.tutor.appointment.model.vo.UserMeVO;
import com.ai.tutor.appointment.model.vo.UserSimpleVO;

import java.util.List;

public interface UserReadService {

    UserMeVO getMe(Long userId);

    List<UserSimpleVO> batch(List<Long> ids);

    UserCardVO getUserCard(Long currentUserId, Long targetUserId);

    String getPhoneByUserId(Long userId);

    void ensurePhoneNotOccupiedByOther(String phone, Long currentUserId);
}
