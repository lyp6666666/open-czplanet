package com.ai.tutor.admin.service;

import com.ai.tutor.admin.model.dto.AdminUserCreateRequest;
import com.ai.tutor.admin.model.dto.AdminUserUpdateRequest;
import com.ai.tutor.admin.model.vo.AdminUserDetailVO;
import com.ai.tutor.admin.model.vo.AdminUserRowVO;
import com.ai.tutor.admin.model.vo.PageResult;

public interface AdminUserManageService {

    PageResult<AdminUserRowVO> pageTeachers(String q, int page, int size);

    PageResult<AdminUserRowVO> pageStudents(String q, int page, int size);

    AdminUserDetailVO getDetail(Long id);

    Long create(AdminUserCreateRequest request);

    void update(Long id, AdminUserUpdateRequest request);

    void disable(Long id);
}

