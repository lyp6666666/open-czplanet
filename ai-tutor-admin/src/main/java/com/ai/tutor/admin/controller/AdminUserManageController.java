package com.ai.tutor.admin.controller;

import com.ai.tutor.admin.model.dto.AdminUserCreateRequest;
import com.ai.tutor.admin.model.dto.AdminUserUpdateRequest;
import com.ai.tutor.admin.model.vo.AdminUserDetailVO;
import com.ai.tutor.admin.model.vo.AdminUserRowVO;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminUserManageService;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.utils.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin User Management", description = "Teacher/Student CRUD")
public class AdminUserManageController {

    @Resource
    private AdminUserManageService adminUserManageService;

    @GetMapping("/teachers")
    @Operation(summary = "Page Teachers")
    public BaseResponse<PageResult<AdminUserRowVO>> pageTeachers(@RequestParam(value = "q", required = false) String q,
                                                                 @RequestParam(value = "page", defaultValue = "1") int page,
                                                                 @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResultUtils.success(adminUserManageService.pageTeachers(q, page, size));
    }

    @GetMapping("/students")
    @Operation(summary = "Page Students")
    public BaseResponse<PageResult<AdminUserRowVO>> pageStudents(@RequestParam(value = "q", required = false) String q,
                                                                 @RequestParam(value = "page", defaultValue = "1") int page,
                                                                 @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResultUtils.success(adminUserManageService.pageStudents(q, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get User Detail")
    public BaseResponse<AdminUserDetailVO> getDetail(@PathVariable("id") Long id) {
        return ResultUtils.success(adminUserManageService.getDetail(id));
    }

    @PostMapping
    @Operation(summary = "Create User")
    public BaseResponse<Long> create(@RequestBody AdminUserCreateRequest request) {
        return ResultUtils.success(adminUserManageService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update User")
    public BaseResponse<Boolean> update(@PathVariable("id") Long id, @RequestBody AdminUserUpdateRequest request) {
        adminUserManageService.update(id, request);
        return ResultUtils.success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Disable User")
    public BaseResponse<Boolean> disable(@PathVariable("id") Long id) {
        adminUserManageService.disable(id);
        return ResultUtils.success(true);
    }
}

