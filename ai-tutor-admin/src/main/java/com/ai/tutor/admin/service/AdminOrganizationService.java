package com.ai.tutor.admin.service;

import com.ai.tutor.admin.model.dto.AdminOrganizationCreateRequest;
import com.ai.tutor.admin.model.dto.AdminOrganizationUpdateRequest;
import com.ai.tutor.admin.model.vo.AdminOrganizationCreateResponse;
import com.ai.tutor.admin.model.vo.AdminOrganizationDetailVO;
import com.ai.tutor.admin.model.vo.AdminOrganizationRowVO;
import com.ai.tutor.admin.model.vo.PageResult;

public interface AdminOrganizationService {

    AdminOrganizationCreateResponse create(AdminOrganizationCreateRequest request);

    PageResult<AdminOrganizationRowVO> page(String q, int page, int size);

    AdminOrganizationDetailVO getDetail(Long orgUserId);

    void update(Long orgUserId, AdminOrganizationUpdateRequest request);

    void disable(Long orgUserId);
}
