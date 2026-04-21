package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.TeacherProfileMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.parent.TutorBrowseRow;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.parent.ParentTutorVOs;
import com.ai.tutor.appointment.service.ParentTutorBrowseService;
import com.ai.tutor.appointment.utils.CityCatalog;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ParentTutorBrowseServiceImpl implements ParentTutorBrowseService {

    @Resource
    private TeacherProfileMapper teacherProfileMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public CursorPageResponse<ParentTutorVOs.TutorCardVO> pageTutors(Long uid,
                                                                    String q,
                                                                    String city,
                                                                    String subject,
                                                                    BigDecimal rateMin,
                                                                    BigDecimal rateMax,
                                                                    CursorPageRequest pageRequest) {
        ThrowUtils.throwIf(uid == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(pageRequest == null, ErrorCode.PARAMS_ERROR);
        User me = userMapper.selectById(uid);
        ThrowUtils.throwIf(me == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(me.getUserType() == null || (me.getUserType() != 2 && me.getUserType() != 3), ErrorCode.NO_AUTH_ERROR);

        String qv = normalize(q);
        String cv = CityCatalog.normalizeCityForFilter(city);
        String sv = normalize(subject);
        Integer pageSize = normalizePageSize(pageRequest.getPageSize());
        Long cursor = pageRequest.getCursor();

        List<TutorBrowseRow> rows = teacherProfileMapper.pageTutorCards(qv, cv, null, sv, rateMin, rateMax, cursor, pageSize);
        if (rows == null || rows.isEmpty()) {
            return new CursorPageResponse<>(null, true, List.of());
        }

        List<ParentTutorVOs.TutorCardVO> list = new ArrayList<>();
        for (TutorBrowseRow r : rows) {
            if (r == null || r.getUserId() == null) continue;
            list.add(new ParentTutorVOs.TutorCardVO(
                    r.getUserId(),
                    buildDisplayName(r),
                    normalize(r.getAvatar()),
                    normalize(r.getCity()),
                    normalize(r.getEducation()),
                    r.getExperienceYears(),
                    r.getRatePerHour() == null ? null : r.getRatePerHour().stripTrailingZeros().toPlainString(),
                    buildSubjectTags(r.getSubject()),
                    buildHighlights(r.getRealnameVerifyStatus(), r.getEduVerifyStatus()),
                    shorten(normalize(r.getIntroduction()), 80)
            ));
        }

        Long nextCursor = rows.get(rows.size() - 1).getId();
        boolean isLast = rows.size() < pageSize;
        return new CursorPageResponse<>(nextCursor, isLast, list);
    }

    private Integer normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return 10;
        }
        return Math.max(1, Math.min(pageSize, 100));
    }

    private String normalize(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        return v.isEmpty() ? null : v;
    }

    private String shorten(String raw, int max) {
        if (raw == null) return null;
        if (raw.length() <= max) return raw;
        return raw.substring(0, max) + "...";
    }

    private String buildDisplayName(TutorBrowseRow r) {
        String real = normalize(r.getRealName());
        if (real != null) return real;
        String name = normalize(r.getUserName());
        if (name != null) return name;
        return "教师" + r.getUserId();
    }

    private List<String> buildSubjectTags(String subjectRaw) {
        String raw = normalize(subjectRaw);
        if (raw == null) return List.of();
        if (raw.contains(",")) {
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .limit(5)
                    .toList();
        }
        if (raw.contains("，")) {
            return Arrays.stream(raw.split("，"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .limit(5)
                    .toList();
        }
        return List.of(raw);
    }

    private List<String> buildHighlights(Integer realnameVerifyStatus, Integer eduVerifyStatus) {
        List<String> list = new ArrayList<>();
        if (realnameVerifyStatus != null && eduVerifyStatus != null && realnameVerifyStatus == 2 && eduVerifyStatus == 2) {
            list.add("实名认证");
            list.add("学籍认证");
        }
        list.add("响应快");
        return list;
    }
}
