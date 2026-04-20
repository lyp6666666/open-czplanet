package com.ai.tutor.admin.mapper;

import com.ai.tutor.admin.model.entity.RefundRequestRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminRefundRequestMapper {

    @Select("SELECT " +
            "  id, brokerage_order_id AS brokerageOrderId, course_id AS courseId, room_id AS roomId, " +
            "  applicant_uid AS applicantUid, applicant_role AS applicantRole, type, status, reason, " +
            "  evidence_images_json AS evidenceImagesJson, evidence_video_url AS evidenceVideoUrl, " +
            "  evidence_video_duration_seconds AS evidenceVideoDurationSeconds, evidence_video_delete_status AS evidenceVideoDeleteStatus, " +
            "  evidence_video_deleted_at AS evidenceVideoDeletedAt, refund_percent AS refundPercent, refund_amount_fen AS refundAmountFen, " +
            "  admin_uid AS adminUid, admin_note AS adminNote, decided_at AS decidedAt, create_time AS createTime, update_time AS updateTime " +
            "FROM refund_request " +
            "WHERE (#{type} IS NULL OR type = #{type}) " +
            "  AND (#{status} IS NULL OR status = #{status}) " +
            "ORDER BY create_time DESC " +
            "LIMIT #{offset}, #{limit}")
    List<RefundRequestRecord> list(@Param("offset") long offset,
                                  @Param("limit") long limit,
                                  @Param("type") String type,
                                  @Param("status") String status);

    @Select("SELECT COUNT(*) " +
            "FROM refund_request " +
            "WHERE (#{type} IS NULL OR type = #{type}) " +
            "  AND (#{status} IS NULL OR status = #{status})")
    long count(@Param("type") String type, @Param("status") String status);

    @Select("SELECT " +
            "  id, brokerage_order_id AS brokerageOrderId, course_id AS courseId, room_id AS roomId, " +
            "  applicant_uid AS applicantUid, applicant_role AS applicantRole, type, status, reason, " +
            "  evidence_images_json AS evidenceImagesJson, evidence_video_url AS evidenceVideoUrl, " +
            "  evidence_video_duration_seconds AS evidenceVideoDurationSeconds, evidence_video_delete_status AS evidenceVideoDeleteStatus, " +
            "  evidence_video_deleted_at AS evidenceVideoDeletedAt, refund_percent AS refundPercent, refund_amount_fen AS refundAmountFen, " +
            "  admin_uid AS adminUid, admin_note AS adminNote, decided_at AS decidedAt, create_time AS createTime, update_time AS updateTime " +
            "FROM refund_request " +
            "WHERE id = #{id} " +
            "LIMIT 1")
    RefundRequestRecord selectById(@Param("id") Long id);

    @Update("UPDATE refund_request " +
            "SET status = 'APPROVED', " +
            "    admin_uid = #{adminUid}, " +
            "    admin_note = #{adminNote}, " +
            "    decided_at = #{decidedAt}, " +
            "    update_time = NOW() " +
            "WHERE id = #{id} " +
            "  AND status = 'PENDING'")
    int approve(@Param("id") Long id,
                @Param("adminUid") Long adminUid,
                @Param("adminNote") String adminNote,
                @Param("decidedAt") LocalDateTime decidedAt);

    @Update("UPDATE refund_request " +
            "SET evidence_video_delete_status = 'DELETED', " +
            "    evidence_video_deleted_at = #{deletedAt}, " +
            "    update_time = NOW() " +
            "WHERE id = #{id} " +
            "  AND evidence_video_url IS NOT NULL")
    int markEvidenceVideoDeleted(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    @Update("UPDATE refund_request " +
            "SET status = 'REJECTED', " +
            "    admin_uid = #{adminUid}, " +
            "    admin_note = #{adminNote}, " +
            "    decided_at = #{decidedAt}, " +
            "    update_time = NOW() " +
            "WHERE id = #{id} " +
            "  AND status = 'PENDING'")
    int reject(@Param("id") Long id,
               @Param("adminUid") Long adminUid,
               @Param("adminNote") String adminNote,
               @Param("decidedAt") LocalDateTime decidedAt);

    @Update("UPDATE refund_request " +
            "SET evidence_video_delete_status = 'KEEP', " +
            "    update_time = NOW() " +
            "WHERE id = #{id} " +
            "  AND evidence_video_url IS NOT NULL " +
            "  AND evidence_video_delete_status = 'PENDING_DELETE'")
    int markEvidenceVideoKeep(@Param("id") Long id);

    @Update("UPDATE brokerage_order " +
            "SET status = 'REFUNDED', " +
            "    refunded_amount_fen = #{refundedAmountFen}, " +
            "    update_time = NOW() " +
            "WHERE id = #{orderId} " +
            "  AND status IN ('REFUND_REVIEW', 'TRIAL_REFUND_REVIEW')")
    int markOrderRefunded(@Param("orderId") Long orderId, @Param("refundedAmountFen") Long refundedAmountFen);

    @Update("UPDATE brokerage_order " +
            "SET status = 'PAID', " +
            "    refund_locked = 0, " +
            "    update_time = NOW() " +
            "WHERE id = #{orderId} " +
            "  AND status IN ('REFUND_REVIEW', 'TRIAL_REFUND_REVIEW')")
    int rollbackOrderPaid(@Param("orderId") Long orderId);

    @Update("UPDATE course_enrollment " +
            "SET status = 'REFUNDED', update_time = NOW() " +
            "WHERE id = #{courseId}")
    int markCourseRefundedById(@Param("courseId") Long courseId);

    @Update("UPDATE course_enrollment " +
            "SET status = 'REFUNDED', update_time = NOW() " +
            "WHERE room_id = #{roomId}")
    int markCourseRefundedByRoomId(@Param("roomId") Long roomId);

    @Update("UPDATE course_enrollment " +
            "SET status = 'COMMUNICATING', update_time = NOW() " +
            "WHERE id = #{courseId} " +
            "  AND status IN ('REFUND_REVIEW', 'TRIAL_REFUND_REVIEW')")
    int rollbackCourseCommunicatingById(@Param("courseId") Long courseId);

    @Update("UPDATE course_enrollment " +
            "SET status = 'COMMUNICATING', update_time = NOW() " +
            "WHERE room_id = #{roomId} " +
            "  AND status IN ('REFUND_REVIEW', 'TRIAL_REFUND_REVIEW')")
    int rollbackCourseCommunicatingByRoomId(@Param("roomId") Long roomId);
}
