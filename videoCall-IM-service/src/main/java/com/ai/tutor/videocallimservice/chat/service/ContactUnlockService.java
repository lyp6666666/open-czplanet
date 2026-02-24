package com.ai.tutor.videocallimservice.chat.service;

import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import com.ai.tutor.videocallimservice.chat.domain.entity.BrokerageOrder;
import com.ai.tutor.videocallimservice.chat.domain.entity.CollaborationProposal;
import com.ai.tutor.videocallimservice.chat.domain.entity.Room;
import com.ai.tutor.videocallimservice.chat.domain.enums.BrokerageOrderStatus;
import com.ai.tutor.videocallimservice.chat.domain.enums.CollaborationProposalStatus;
import com.ai.tutor.videocallimservice.chat.domain.vo.response.UnlockedContactVO;
import com.ai.tutor.videocallimservice.chat.mapper.BrokerageOrderMapper;
import com.ai.tutor.videocallimservice.chat.mapper.CollaborationProposalMapper;
import com.ai.tutor.videocallimservice.chat.mapper.RoomMapper;
import com.ai.tutor.videocallimservice.common.mapper.StudentProfileLiteMapper;
import com.ai.tutor.videocallimservice.common.mapper.TeacherProfileLiteMapper;
import jakarta.annotation.Resource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ContactUnlockService {

    @Resource
    private RoomMapper roomMapper;
    @Resource
    private TeacherProfileLiteMapper teacherProfileLiteMapper;
    @Resource
    private StudentProfileLiteMapper studentProfileLiteMapper;
    @Resource
    private CollaborationProposalMapper collaborationProposalMapper;
    @Resource
    private BrokerageOrderMapper brokerageOrderMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    public UnlockedContactVO getUnlockedContact(Long roomId, Long targetUid, Long uid) {
        ThrowUtils.throwIf(roomId == null || targetUid == null || uid == null, ErrorCode.PARAMS_ERROR);
        Room room = roomMapper.selectById(roomId);
        ThrowUtils.throwIf(room == null || room.getStatus() == null || room.getStatus() != 1, ErrorCode.NOT_FOUND_ERROR);
        Long teacherUid = teacherProfileLiteMapper.selectUserIdById(room.getTeacherProfileId());
        Long studentUid = studentProfileLiteMapper.selectUserIdById(room.getStudentProfileId());
        ThrowUtils.throwIf(teacherUid == null || studentUid == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(teacherUid), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(!targetUid.equals(studentUid), ErrorCode.NO_AUTH_ERROR);

        CollaborationProposal proposal = collaborationProposalMapper.selectLatestByRoomId(roomId);
        ThrowUtils.throwIf(proposal == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!CollaborationProposalStatus.ACCEPTED.name().equals(proposal.getStatus()), ErrorCode.NO_AUTH_ERROR);

        BrokerageOrder order = brokerageOrderMapper.selectByProposalId(proposal.getId());
        ThrowUtils.throwIf(order == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!BrokerageOrderStatus.PAID.name().equals(order.getStatus()), ErrorCode.NO_AUTH_ERROR);

        String phone = "";
        try {
            phone = jdbcTemplate.queryForObject(
                    "SELECT phone FROM user WHERE id = ? LIMIT 1",
                    new Object[]{targetUid},
                    (rs, rowNum) -> rs.getString("phone")
            );
        } catch (EmptyResultDataAccessException ignored) {
        }
        return UnlockedContactVO.builder().uid(targetUid).phone(phone == null ? "" : phone).build();
    }
}
