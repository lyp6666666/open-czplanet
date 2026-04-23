package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.vo.admin.EmailTaskDetailVO;
import com.ai.tutor.appointment.model.vo.admin.EmailTaskRowVO;
import com.ai.tutor.appointment.model.vo.admin.PageResult;

public interface EmailAdminService {
    PageResult<EmailTaskRowVO> pageTasks(int page, int size, Long userId, String email, String templateCode, String bizType, String status);

    EmailTaskDetailVO getTaskDetail(Long taskId);

    boolean retryTask(Long taskId);
}
