package com.ai.tutor.admin.service.impl;

import com.ai.tutor.admin.integration.feign.PaymentRefundFeignClient;
import com.ai.tutor.admin.mapper.AdminMessageMapper;
import com.ai.tutor.admin.mapper.AdminRefundMapper;
import com.ai.tutor.admin.mapper.AdminRefundRequestMapper;
import com.ai.tutor.admin.mapper.UserMapper;
import com.ai.tutor.admin.model.dto.PaymentRefundRequest;
import com.ai.tutor.admin.model.dto.PaymentRefundResponse;
import com.ai.tutor.admin.model.entity.BrokerageOrder;
import com.ai.tutor.admin.model.entity.Message;
import com.ai.tutor.admin.model.entity.RefundRequestRecord;
import com.ai.tutor.admin.model.entity.User;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.model.vo.RefundChatParticipantVO;
import com.ai.tutor.admin.model.vo.RefundRequestDetailResponse;
import com.ai.tutor.admin.service.AdminRefundRequestService;
import com.ai.tutor.admin.storage.MinioProperties;
import com.ai.tutor.common.BaseResponse;
import com.ai.tutor.common.metrics.BizKpiMetrics;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.exception.BusinessException;
import com.ai.tutor.utils.ThrowUtils;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminRefundRequestServiceImpl implements AdminRefundRequestService {

    @Resource
    private AdminRefundRequestMapper adminRefundRequestMapper;
    @Resource
    private AdminRefundMapper adminRefundMapper;
    @Resource
    private AdminMessageMapper adminMessageMapper;
    @Resource
    private PaymentRefundFeignClient paymentRefundFeignClient;
    @Resource
    private UserMapper userMapper;
    @Resource
    private MinioClient minioClient;
    @Resource
    private MinioProperties minioProperties;
    @Resource
    private BizKpiMetrics bizKpiMetrics;

    @Override
    public PageResult<RefundRequestRecord> list(int page, int size, String type, String status) {
        int p = Math.max(1, page);
        int s = Math.max(1, Math.min(size <= 0 ? 20 : size, 50));
        long offset = (long) (p - 1) * s;
        List<RefundRequestRecord> records = adminRefundRequestMapper.list(offset, s, blankToNull(type), blankToNull(status));
        long total = adminRefundRequestMapper.count(blankToNull(type), blankToNull(status));
        return PageResult.<RefundRequestRecord>builder()
                .records(records)
                .total(total)
                .size(s)
                .current(p)
                .build();
    }

    @Override
    public RefundRequestDetailResponse detail(Long requestId) {
        ThrowUtils.throwIf(requestId == null, ErrorCode.PARAMS_ERROR);
        RefundRequestRecord request = adminRefundRequestMapper.selectById(requestId);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "退款申请不存在");

        BrokerageOrder order = request.getBrokerageOrderId() == null ? null : adminRefundMapper.selectById(request.getBrokerageOrderId());

        List<Message> chatHistory = null;
        if (request.getRoomId() != null) {
            chatHistory = adminMessageMapper.listByRoomId(request.getRoomId());
        }

        Map<Long, User> usersById = loadRelatedUsers(request, order, chatHistory);
        RefundChatParticipantVO studentParticipant = resolveParticipant(usersById, request, order, chatHistory, 2, "STUDENT");
        RefundChatParticipantVO teacherParticipant = resolveParticipant(usersById, request, order, chatHistory, 1, "TEACHER");

        return RefundRequestDetailResponse.builder()
                .refundRequest(request)
                .order(order)
                .chatHistory(chatHistory)
                .studentParticipant(studentParticipant)
                .teacherParticipant(teacherParticipant)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long requestId, Long adminUid, String note) {
        ThrowUtils.throwIf(requestId == null || adminUid == null, ErrorCode.PARAMS_ERROR);
        RefundRequestRecord request = adminRefundRequestMapper.selectById(requestId);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "退款申请不存在");
        if (!"PENDING".equals(request.getStatus())) {
            return;
        }
        ThrowUtils.throwIf(request.getBrokerageOrderId() == null, ErrorCode.OPERATION_ERROR, "退款申请缺少订单关联");
        ThrowUtils.throwIf(request.getRefundAmountFen() == null || request.getRefundAmountFen() <= 0, ErrorCode.OPERATION_ERROR, "退款金额非法");

        PaymentRefundRequest payReq = new PaymentRefundRequest();
        payReq.setContextType("BROKERAGE_ORDER");
        payReq.setContextId(request.getBrokerageOrderId());
        payReq.setRequestId(request.getId());
        payReq.setRefundAmountFen(request.getRefundAmountFen());
        String reason = request.getReason() == null || request.getReason().trim().isEmpty() ? "退款" : request.getReason().trim();
        payReq.setReason(reason);

        BaseResponse<PaymentRefundResponse> resp = paymentRefundFeignClient.refund(payReq);
        if (resp == null || resp.getCode() != 0 || resp.getData() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, resp == null ? "退款失败" : resp.getMessage());
        }
        if ("FAILED".equalsIgnoreCase(resp.getData().getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "退款失败：" + resp.getData().getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = adminRefundRequestMapper.approve(requestId, adminUid, trimTo1024(note), now);
        if (updated <= 0) {
            return;
        }
        if (bizKpiMetrics != null) {
            /*
             * 中文注释：管理端退款审批指标只统计“审核动作”本身，最终退款成功仍以后端支付域的成功回执为准，
             * 避免同一笔退款在审批成功和支付成功两个节点被重复记成最终退款成功。
             */
            bizKpiMetrics.incRefundReview("approved");
        }
        adminRefundRequestMapper.markOrderRefunded(request.getBrokerageOrderId(), request.getRefundAmountFen());
        deleteEvidenceVideoIfNeeded(request, now);
        if (request.getCourseId() != null) {
            adminRefundRequestMapper.markCourseRefundedById(request.getCourseId());
        } else if (request.getRoomId() != null) {
            adminRefundRequestMapper.markCourseRefundedByRoomId(request.getRoomId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long requestId, Long adminUid, String reason) {
        ThrowUtils.throwIf(requestId == null || adminUid == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(reason == null || reason.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "拒绝原因不能为空");
        RefundRequestRecord request = adminRefundRequestMapper.selectById(requestId);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "退款申请不存在");
        if (!"PENDING".equals(request.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int updated = adminRefundRequestMapper.reject(requestId, adminUid, trimTo1024(reason), now);
        if (updated <= 0) {
            return;
        }
        if (bizKpiMetrics != null) {
            /*
             * 中文注释：退款拒绝也需要单独统计，用来观察争议申请里被运营或风控驳回的比例变化。
             */
            bizKpiMetrics.incRefundReview("rejected");
        }
        adminRefundRequestMapper.markEvidenceVideoKeep(requestId);
        if (request.getBrokerageOrderId() != null) {
            adminRefundRequestMapper.rollbackOrderPaid(request.getBrokerageOrderId());
        }
        if (request.getCourseId() != null) {
            adminRefundRequestMapper.rollbackCourseCommunicatingById(request.getCourseId());
        } else if (request.getRoomId() != null) {
            adminRefundRequestMapper.rollbackCourseCommunicatingByRoomId(request.getRoomId());
        }
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private static String trimTo1024(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty()) return null;
        if (v.length() <= 1024) return v;
        return v.substring(0, 1024);
    }

    private void deleteEvidenceVideoIfNeeded(RefundRequestRecord request, LocalDateTime now) {
        if (request == null) {
            return;
        }
        String objectKey = resolveObjectKey(request.getEvidenceVideoUrl());
        if (objectKey != null && minioProperties.isEnabled()) {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(objectKey)
                        .build());
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "录屏删除失败，请稍后重试");
            }
        }
        adminRefundRequestMapper.markEvidenceVideoDeleted(request.getId(), now);
    }

    private static String resolveObjectKey(String evidenceVideoUrl) {
        String value = evidenceVideoUrl == null ? "" : evidenceVideoUrl.trim();
        if (value.isEmpty()) {
            return null;
        }
        String marker = "/api/v1/public/assets/";
        int idx = value.indexOf(marker);
        if (idx >= 0) {
            String key = value.substring(idx + marker.length()).trim();
            return key.isEmpty() ? null : key;
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return value.startsWith("/") ? value.substring(1) : value;
        }
        return null;
    }

    private Map<Long, User> loadRelatedUsers(RefundRequestRecord request, BrokerageOrder order, List<Message> chatHistory) {
        Set<Long> ids = new LinkedHashSet<>();
        addUid(ids, request == null ? null : request.getApplicantUid());
        addUid(ids, order == null ? null : order.getPayerUid());
        if (chatHistory != null) {
            for (Message message : chatHistory) {
                if (message == null) {
                    continue;
                }
                addUid(ids, message.getFromUid());
                addUid(ids, message.getToUid());
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectByIds(new ArrayList<>(ids));
        Map<Long, User> map = new LinkedHashMap<>();
        if (users != null) {
            for (User user : users) {
                if (user != null && user.getId() != null) {
                    map.put(user.getId(), user);
                }
            }
        }
        return map;
    }

    private RefundChatParticipantVO resolveParticipant(Map<Long, User> usersById,
                                                       RefundRequestRecord request,
                                                       BrokerageOrder order,
                                                       List<Message> chatHistory,
                                                       int expectedUserType,
                                                       String role) {
        User matchedUser = findFirstByUserType(usersById, expectedUserType);
        if (matchedUser != null) {
            return toParticipant(matchedUser.getId(), matchedUser, role);
        }

        if (request != null && role.equalsIgnoreCase(request.getApplicantRole())) {
            User user = usersById.get(request.getApplicantUid());
            return toParticipant(request.getApplicantUid(), user, role);
        }

        if ("STUDENT".equals(role) && order != null && order.getPayerUid() != null) {
            User user = usersById.get(order.getPayerUid());
            return toParticipant(order.getPayerUid(), user, role);
        }

        if (chatHistory != null) {
            for (Message message : chatHistory) {
                if (message == null) {
                    continue;
                }
                RefundChatParticipantVO from = toParticipantIfUsable(message.getFromUid(), usersById, expectedUserType, role);
                if (from != null) {
                    return from;
                }
                RefundChatParticipantVO to = toParticipantIfUsable(message.getToUid(), usersById, expectedUserType, role);
                if (to != null) {
                    return to;
                }
            }
        }

        Long fallbackUid = null;
        if (request != null && request.getApplicantUid() != null) {
            fallbackUid = request.getApplicantUid();
        } else if (order != null && order.getPayerUid() != null) {
            fallbackUid = order.getPayerUid();
        } else if (chatHistory != null) {
            for (Message message : chatHistory) {
                if (message != null && message.getFromUid() != null) {
                    fallbackUid = message.getFromUid();
                    break;
                }
            }
        }
        return toParticipant(fallbackUid, fallbackUid == null ? null : usersById.get(fallbackUid), role);
    }

    private RefundChatParticipantVO toParticipantIfUsable(Long uid, Map<Long, User> usersById, int expectedUserType, String role) {
        if (uid == null) {
            return null;
        }
        User user = usersById.get(uid);
        if (user == null) {
            return null;
        }
        if (user.getUserType() != null && user.getUserType() != expectedUserType) {
            return null;
        }
        return toParticipant(uid, user, role);
    }

    private User findFirstByUserType(Map<Long, User> usersById, int userType) {
        for (User user : usersById.values()) {
            if (user != null && user.getUserType() != null && user.getUserType() == userType) {
                return user;
            }
        }
        return null;
    }

    private RefundChatParticipantVO toParticipant(Long uid, User user, String fallbackRole) {
        if (uid == null && user == null) {
            return null;
        }
        String role = fallbackRole;
        if (user != null && user.getUserType() != null) {
            if (user.getUserType() == 1) {
                role = "TEACHER";
            } else if (user.getUserType() == 2) {
                role = "STUDENT";
            }
        }
        return RefundChatParticipantVO.builder()
                .uid(uid != null ? uid : user.getId())
                .role(role)
                .name(user == null ? null : user.getName())
                .avatar(user == null ? null : user.getAvatar())
                .build();
    }

    private void addUid(Set<Long> ids, Long uid) {
        if (uid != null) {
            ids.add(uid);
        }
    }
}
