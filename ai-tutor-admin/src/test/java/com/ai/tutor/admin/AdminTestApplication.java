package com.ai.tutor.admin;

import com.ai.tutor.admin.model.dto.AdminLoginRequest;
import com.ai.tutor.admin.model.dto.AdminInviteSystemConfigRequest;
import com.ai.tutor.admin.model.entity.BrokerageOrder;
import com.ai.tutor.admin.model.vo.AdminLoginResponse;
import com.ai.tutor.admin.model.vo.DashboardStatsResponse;
import com.ai.tutor.admin.model.vo.DisputeDetailResponse;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminAuthService;
import com.ai.tutor.admin.service.AdminDashboardService;
import com.ai.tutor.admin.service.AdminHomeCarouselService;
import com.ai.tutor.admin.service.AdminInviteService;
import com.ai.tutor.admin.service.AdminJobService;
import com.ai.tutor.admin.service.AdminPaymentRecordService;
import com.ai.tutor.admin.service.AdminRefundService;
import com.ai.tutor.admin.service.AdminRefundRequestService;
import com.ai.tutor.admin.service.AdminOrganizationService;
import com.ai.tutor.admin.service.AdminUserManageService;
import com.ai.tutor.admin.service.AdminVerificationService;
import com.ai.tutor.admin.model.entity.PaymentOrderRecord;
import com.ai.tutor.admin.model.entity.StudentJobPosting;
import com.ai.tutor.admin.model.entity.TeacherProfile;
import com.ai.tutor.admin.model.entity.RefundRequestRecord;
import com.ai.tutor.admin.model.vo.AdminHomeCarouselItemVO;
import com.ai.tutor.admin.model.vo.AdminInviteSystemConfigVO;
import com.ai.tutor.admin.model.vo.RefundRequestDetailResponse;
import com.ai.tutor.admin.storage.MinioProperties;
import io.minio.MinioClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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
    public MinioClient minioClient() {
        return Mockito.mock(MinioClient.class);
    }

    @Bean
    public MinioProperties minioProperties() {
        MinioProperties properties = new MinioProperties();
        properties.setBucket("ai-tutor-assets");
        return properties;
    }

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
    public AdminHomeCarouselService adminHomeCarouselService() {
        return new AdminHomeCarouselService() {
            @Override
            public List<AdminHomeCarouselItemVO> list() {
                return Collections.emptyList();
            }

            @Override
            public AdminHomeCarouselItemVO create(String title, String subtitle, String linkUrl, MultipartFile file, Long adminUid) {
                return null;
            }

            @Override
            public void delete(Long id, Long adminUid) {
            }
        };
    }

    @Bean
    public AdminInviteService adminInviteService() {
        return new AdminInviteService() {
            @Override
            public PageResult<com.ai.tutor.admin.model.vo.AdminInviteRelationVO> listRelations(int page, int size, Long inviterUid, Long inviteeUid, String status) {
                return PageResult.<com.ai.tutor.admin.model.vo.AdminInviteRelationVO>builder()
                        .records(Collections.emptyList())
                        .total(0)
                        .size(size)
                        .current(page)
                        .build();
            }

            @Override
            public PageResult<com.ai.tutor.admin.model.vo.AdminInviteRewardVO> listRewards(int page, int size, Long inviterUid, Long inviteeUid, String status, String scene, String settlementMonth) {
                return PageResult.<com.ai.tutor.admin.model.vo.AdminInviteRewardVO>builder()
                        .records(Collections.emptyList())
                        .total(0)
                        .size(size)
                        .current(page)
                        .build();
            }

            @Override
            public PageResult<com.ai.tutor.admin.model.vo.AdminInviteSettlementVO> listSettlements(int page, int size, Long userId, String status, String settlementMonth) {
                return PageResult.<com.ai.tutor.admin.model.vo.AdminInviteSettlementVO>builder()
                        .records(Collections.emptyList())
                        .total(0)
                        .size(size)
                        .current(page)
                        .build();
            }

            @Override
            public void markSettlementPaid(Long settlementId) {
            }

            @Override
            public void markSettlementFailed(Long settlementId, String reason) {
            }

            @Override
            public AdminInviteSystemConfigVO systemConfig() {
                return AdminInviteSystemConfigVO.builder()
                        .enabled(true)
                        .systemInviteCode("CHUANGZHI")
                        .systemInviteLink("http://localhost:5173/auth/student?inviteCode=CHUANGZHI")
                        .tutorInfoFeeDiscountRate(0.5D)
                        .studentRewardRate(0.13D)
                        .promoTitle("创智推广专属福利")
                        .promoDesc("desc")
                        .build();
            }

            @Override
            public AdminInviteSystemConfigVO saveSystemConfig(AdminInviteSystemConfigRequest request) {
                return systemConfig();
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
    public AdminRefundRequestService adminRefundRequestService() {
        return new AdminRefundRequestService() {
            @Override
            public PageResult<RefundRequestRecord> list(int page, int size, String type, String status) {
                return PageResult.<RefundRequestRecord>builder()
                        .records(Collections.emptyList())
                        .total(0)
                        .size(size)
                        .current(page)
                        .build();
            }

            @Override
            public RefundRequestDetailResponse detail(Long requestId) {
                return null;
            }

            @Override
            public void approve(Long requestId, Long adminUid, String note) {
            }

            @Override
            public void reject(Long requestId, Long adminUid, String reason) {
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
