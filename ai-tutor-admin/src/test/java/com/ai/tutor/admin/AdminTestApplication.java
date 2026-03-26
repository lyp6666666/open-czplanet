package com.ai.tutor.admin;

import com.ai.tutor.admin.model.dto.AdminLoginRequest;
import com.ai.tutor.admin.model.entity.BrokerageOrder;
import com.ai.tutor.admin.model.vo.AdminLoginResponse;
import com.ai.tutor.admin.model.vo.DashboardStatsResponse;
import com.ai.tutor.admin.model.vo.DisputeDetailResponse;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminAuthService;
import com.ai.tutor.admin.service.AdminDashboardService;
import com.ai.tutor.admin.service.AdminJobService;
import com.ai.tutor.admin.service.AdminPaymentRecordService;
import com.ai.tutor.admin.service.AdminRefundService;
import com.ai.tutor.admin.service.AdminOrganizationService;
import com.ai.tutor.admin.service.AdminUserManageService;
import com.ai.tutor.admin.service.AdminVerificationService;
import com.ai.tutor.admin.model.entity.PaymentOrderRecord;
import com.ai.tutor.admin.model.entity.StudentJobPosting;
import com.ai.tutor.admin.model.entity.TeacherProfile;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.Collections;

@SpringBootApplication(
        scanBasePackages = {
                "com.ai.tutor.admin.controller",
                "com.ai.tutor.admin.config",
                "com.ai.tutor.admin.interceptor",
                "com.ai.tutor.admin.utils",
                "com.ai.tutor.common.handler"
        },
        excludeName = {
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
                "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration",
                "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
        }
)
public class AdminTestApplication {

    @Bean
    public AdminAuthService adminAuthService() {
        return new AdminAuthService() {
            @Override
            public AdminLoginResponse login(AdminLoginRequest request) {
                return null;
            }
        };
    }

    @Bean
    public AdminDashboardService adminDashboardService() {
        return new AdminDashboardService() {
            @Override
            public DashboardStatsResponse getStats() {
                return null;
            }
        };
    }

    @Bean
    public AdminJobService adminJobService() {
        return new AdminJobService() {
            @Override
            public PageResult<StudentJobPosting> listPendingJobs(int page, int size) {
                return PageResult.<StudentJobPosting>builder()
                        .records(Collections.emptyList())
                        .total(0)
                        .size(size)
                        .current(page)
                        .build();
            }

            @Override
            public void approveJob(Long id) {
            }

            @Override
            public void rejectJob(Long id, String reason) {
            }
        };
    }

    @Bean
    public AdminPaymentRecordService adminPaymentRecordService() {
        return new AdminPaymentRecordService() {
            @Override
            public PageResult<PaymentOrderRecord> list(int page, int size, String orderNo, Long userId, String contextType, Long contextId, String channel, String status, LocalDateTime startTime, LocalDateTime endTime) {
                return PageResult.<PaymentOrderRecord>builder()
                        .records(Collections.emptyList())
                        .total(0)
                        .size(size)
                        .current(page)
                        .build();
            }

            @Override
            public PaymentOrderRecord detail(String orderNo) {
                return null;
            }
        };
    }

    @Bean
    public AdminUserManageService adminUserManageService() {
        return new AdminUserManageService() {
            @Override
            public PageResult<com.ai.tutor.admin.model.vo.AdminUserRowVO> pageTeachers(String q, int page, int size) {
                return PageResult.<com.ai.tutor.admin.model.vo.AdminUserRowVO>builder()
                        .records(Collections.emptyList())
                        .total(0)
                        .size(size)
                        .current(page)
                        .build();
            }

            @Override
            public PageResult<com.ai.tutor.admin.model.vo.AdminUserRowVO> pageStudents(String q, int page, int size) {
                return PageResult.<com.ai.tutor.admin.model.vo.AdminUserRowVO>builder()
                        .records(Collections.emptyList())
                        .total(0)
                        .size(size)
                        .current(page)
                        .build();
            }

            @Override
            public com.ai.tutor.admin.model.vo.AdminUserDetailVO getDetail(Long id) {
                return null;
            }

            @Override
            public Long create(com.ai.tutor.admin.model.dto.AdminUserCreateRequest request) {
                return null;
            }

            @Override
            public void update(Long id, com.ai.tutor.admin.model.dto.AdminUserUpdateRequest request) {
            }

            @Override
            public void disable(Long id) {
            }
        };
    }

    @Bean
    public AdminOrganizationService adminOrganizationService() {
        return new AdminOrganizationService() {
            @Override
            public com.ai.tutor.admin.model.vo.AdminOrganizationCreateResponse create(com.ai.tutor.admin.model.dto.AdminOrganizationCreateRequest request) {
                return null;
            }

            @Override
            public com.ai.tutor.admin.model.vo.PageResult<com.ai.tutor.admin.model.vo.AdminOrganizationRowVO> page(String q, int page, int size) {
                return com.ai.tutor.admin.model.vo.PageResult.<com.ai.tutor.admin.model.vo.AdminOrganizationRowVO>builder()
                        .records(java.util.Collections.emptyList())
                        .total(0)
                        .size(size)
                        .current(page)
                        .build();
            }

            @Override
            public com.ai.tutor.admin.model.vo.AdminOrganizationDetailVO getDetail(Long orgUserId) {
                return null;
            }

            @Override
            public void update(Long orgUserId, com.ai.tutor.admin.model.dto.AdminOrganizationUpdateRequest request) {
            }

            @Override
            public void disable(Long orgUserId) {
            }
        };
    }

    @Bean
    public AdminVerificationService adminVerificationService() {
        return new AdminVerificationService() {
            @Override
            public PageResult<TeacherProfile> listPendingVerifications(int page, int size) {
                return PageResult.<TeacherProfile>builder()
                        .records(Collections.emptyList())
                        .total(0)
                        .size(size)
                        .current(page)
                        .build();
            }

            @Override
            public TeacherProfile getVerificationDetails(Long userId) {
                return null;
            }

            @Override
            public void approveVerification(Long userId, String type) {
            }

            @Override
            public void rejectVerification(Long userId, String type, String reason) {
            }
        };
    }

    @Bean
    public AdminRefundService adminRefundService() {
        return new AdminRefundService() {
            @Override
            public PageResult<BrokerageOrder> listRefundDisputes(int page, int size) {
                return PageResult.<BrokerageOrder>builder()
                        .records(Collections.emptyList())
                        .total(0)
                        .size(size)
                        .current(page)
                        .build();
            }

            @Override
            public DisputeDetailResponse getDisputeDetails(Long orderId) {
                return null;
            }

            @Override
            public void approveRefund(Long orderId) {
            }

            @Override
            public void rejectRefund(Long orderId, String reason) {
            }
        };
    }
}
