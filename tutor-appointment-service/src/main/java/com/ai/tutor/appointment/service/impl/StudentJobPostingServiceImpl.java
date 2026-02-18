package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.StudentJobPostingMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.enums.PublisherIdentityEnum;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.job.CreateStudentJobPostingRequest;
import com.ai.tutor.appointment.model.dto.job.UpdateStudentJobPostingRequest;
import com.ai.tutor.appointment.model.entity.StudentJobPosting;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.DemandViewVO;
import com.ai.tutor.appointment.service.StudentJobPostingService;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class StudentJobPostingServiceImpl implements StudentJobPostingService {

    @Resource
    private StudentJobPostingMapper studentJobPostingMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public Long create(CreateStudentJobPostingRequest request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);
        validateCreate(request);

        StudentJobPosting posting = StudentJobPosting.builder()
                .parentId(uid)
                .subjectId(request.getSubjectId())
                .title(request.getTitle())
                .description(request.getDescription())
                .childAge(request.getChildAge())
                .classMode(request.getClassMode())
                .city(request.getCity())
                .address(request.getAddress())
                .frequencyPerWeek(request.getFrequencyPerWeek())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .stageCode(request.getStageCode())
                .educationRequirement(normalizeEducationRequirementForStorage(request.getEducationRequirement()))
                .publisherIdentity(normalizePublisherIdentity(request.getPublisherIdentity()))
                .schedule(request.getSchedule())
                .status(1)
                .build();
        int inserted = studentJobPostingMapper.insert(posting);
        ThrowUtils.throwIf(inserted <= 0 || posting.getId() == null, ErrorCode.OPERATION_ERROR);
        return posting.getId();
    }

    @Override
    public void update(Long id, UpdateStudentJobPostingRequest request, Long uid) {
        ThrowUtils.throwIf(id == null || request == null || uid == null, ErrorCode.PARAMS_ERROR);

        StudentJobPosting db = studentJobPostingMapper.selectById(id);
        ThrowUtils.throwIf(db == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(db.getParentId()), ErrorCode.NO_AUTH_ERROR);

        validateUpdate(request, db);

        StudentJobPosting toUpdate = StudentJobPosting.builder()
                .id(id)
                .subjectId(request.getSubjectId())
                .title(request.getTitle())
                .description(request.getDescription())
                .childAge(request.getChildAge())
                .classMode(request.getClassMode())
                .city(request.getCity())
                .address(request.getAddress())
                .frequencyPerWeek(request.getFrequencyPerWeek())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .stageCode(request.getStageCode())
                .educationRequirement(request.getEducationRequirement() == null ? null : normalizeEducationRequirementForStorage(request.getEducationRequirement()))
                .publisherIdentity(request.getPublisherIdentity() == null ? null : normalizePublisherIdentity(request.getPublisherIdentity()))
                .schedule(request.getSchedule())
                .status(request.getStatus())
                .build();
        int updated = studentJobPostingMapper.updateById(toUpdate);
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public StudentJobPosting getById(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        StudentJobPosting posting = studentJobPostingMapper.selectById(id);
        ThrowUtils.throwIf(posting == null, ErrorCode.NOT_FOUND_ERROR);
        return posting;
    }

    @Override
    public DemandViewVO getViewById(Long id) {
        StudentJobPosting posting = getById(id);
        User user = userMapper.selectById(posting.getParentId());
        DemandViewVO.Publisher publisher = DemandViewVO.Publisher.builder()
                .uid(posting.getParentId())
                .displayName(buildDisplayName(user))
                .avatar(user == null ? null : user.getAvatar())
                .identityLabel(resolvePublisherIdentityLabel(posting.getPublisherIdentity()))
                .build();

        return DemandViewVO.builder()
                .id(posting.getId())
                .parentId(posting.getParentId())
                .subjectId(posting.getSubjectId())
                .title(posting.getTitle())
                .description(posting.getDescription())
                .childAge(posting.getChildAge())
                .classMode(posting.getClassMode())
                .city(posting.getCity())
                .address(posting.getAddress())
                .frequencyPerWeek(posting.getFrequencyPerWeek())
                .budgetMin(posting.getBudgetMin())
                .budgetMax(posting.getBudgetMax())
                .stageCode(posting.getStageCode())
                .educationRequirement(posting.getEducationRequirement())
                .publisherIdentity(posting.getPublisherIdentity())
                .schedule(posting.getSchedule())
                .status(posting.getStatus())
                .createTime(posting.getCreateTime())
                .updateTime(posting.getUpdateTime())
                .publisher(publisher)
                .build();
    }

    @Override
    public CursorPageResponse<StudentJobPosting> listMine(CursorPageRequest request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);
        Integer pageSize = request.getPageSize();

        List<StudentJobPosting> list = studentJobPostingMapper.listByParentId(uid, request.getCursor(), pageSize);
        return buildCursorResponse(list, pageSize);
    }

    @Override
    public CursorPageResponse<StudentJobPosting> listPublished(Long subjectId,
                                                              String city,
                                                              String classMode,
                                                              String stageCode,
                                                              Integer frequencyPerWeek,
                                                              String educationRequirement,
                                                              BigDecimal budgetMin,
                                                              BigDecimal budgetMax,
                                                              String keyword,
                                                              String sort,
                                                              CursorPageRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Integer pageSize = request.getPageSize();

        String edu = normalizeEducationRequirementForFilter(educationRequirement);
        List<StudentJobPosting> list = studentJobPostingMapper.listPublishedFiltered(
                subjectId,
                city,
                classMode,
                stageCode,
                frequencyPerWeek,
                edu,
                budgetMin,
                budgetMax,
                keyword,
                sort,
                request.getCursor(),
                pageSize
        );
        return buildCursorResponse(list, pageSize);
    }

    private static final Set<String> CLASS_MODES = Set.of("online", "offline", "both");

    private static void validateCreate(CreateStudentJobPostingRequest request) {
        ThrowUtils.throwIf(request.getSubjectId() == null, ErrorCode.PARAMS_ERROR, "科目不能为空");
        ThrowUtils.throwIf(isBlank(request.getTitle()), ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(isBlank(request.getDescription()), ErrorCode.PARAMS_ERROR, "需求描述不能为空");
        ThrowUtils.throwIf(isBlank(request.getClassMode()), ErrorCode.PARAMS_ERROR, "授课方式不能为空");
        ThrowUtils.throwIf(!CLASS_MODES.contains(request.getClassMode().trim().toLowerCase()), ErrorCode.PARAMS_ERROR, "授课方式不合法");
        ThrowUtils.throwIf(request.getFrequencyPerWeek() == null, ErrorCode.PARAMS_ERROR, "授课频次不能为空");
        ThrowUtils.throwIf(request.getFrequencyPerWeek() < 1 || request.getFrequencyPerWeek() > 7, ErrorCode.PARAMS_ERROR, "授课频次需在 1~7 之间");
        ThrowUtils.throwIf(isBlank(request.getStageCode()), ErrorCode.PARAMS_ERROR, "授课学段不能为空");
        ThrowUtils.throwIf(isBlank(request.getEducationRequirement()), ErrorCode.PARAMS_ERROR, "学历要求不能为空");
        ThrowUtils.throwIf(isBlank(request.getPublisherIdentity()), ErrorCode.PARAMS_ERROR, "发布者身份不能为空");
        ThrowUtils.throwIf(PublisherIdentityEnum.fromCode(request.getPublisherIdentity()) == null, ErrorCode.PARAMS_ERROR, "发布者身份不合法");

        validateModeAddress(request.getClassMode(), request.getCity(), request.getAddress());
        validateBudgetRange(request.getBudgetMin(), request.getBudgetMax());
    }

    private static void validateUpdate(UpdateStudentJobPostingRequest request, StudentJobPosting db) {
        String classMode = firstNonBlank(request.getClassMode(), db.getClassMode());
        String title = firstNonBlank(request.getTitle(), db.getTitle());
        String desc = firstNonBlank(request.getDescription(), db.getDescription());
        Long subjectId = request.getSubjectId() == null ? db.getSubjectId() : request.getSubjectId();
        Integer freq = request.getFrequencyPerWeek() == null ? db.getFrequencyPerWeek() : request.getFrequencyPerWeek();
        String stage = firstNonBlank(request.getStageCode(), db.getStageCode());
        String edu = firstNonBlank(request.getEducationRequirement(), db.getEducationRequirement());
        String pubId = firstNonBlank(request.getPublisherIdentity(), db.getPublisherIdentity());
        String city = request.getCity() == null ? db.getCity() : request.getCity();
        String address = request.getAddress() == null ? db.getAddress() : request.getAddress();
        BigDecimal budgetMin = request.getBudgetMin() == null ? db.getBudgetMin() : request.getBudgetMin();
        BigDecimal budgetMax = request.getBudgetMax() == null ? db.getBudgetMax() : request.getBudgetMax();

        ThrowUtils.throwIf(subjectId == null, ErrorCode.PARAMS_ERROR, "科目不能为空");
        ThrowUtils.throwIf(isBlank(title), ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(isBlank(desc), ErrorCode.PARAMS_ERROR, "需求描述不能为空");
        ThrowUtils.throwIf(isBlank(classMode), ErrorCode.PARAMS_ERROR, "授课方式不能为空");
        ThrowUtils.throwIf(!CLASS_MODES.contains(classMode.trim().toLowerCase()), ErrorCode.PARAMS_ERROR, "授课方式不合法");
        ThrowUtils.throwIf(freq == null || freq < 1 || freq > 7, ErrorCode.PARAMS_ERROR, "授课频次需在 1~7 之间");
        ThrowUtils.throwIf(isBlank(stage), ErrorCode.PARAMS_ERROR, "授课学段不能为空");
        ThrowUtils.throwIf(isBlank(edu), ErrorCode.PARAMS_ERROR, "学历要求不能为空");
        ThrowUtils.throwIf(isBlank(pubId), ErrorCode.PARAMS_ERROR, "发布者身份不能为空");
        ThrowUtils.throwIf(PublisherIdentityEnum.fromCode(pubId) == null, ErrorCode.PARAMS_ERROR, "发布者身份不合法");

        validateModeAddress(classMode, city, address);
        validateBudgetRange(budgetMin, budgetMax);
    }

    private static void validateModeAddress(String classMode, String city, String address) {
        if (classMode == null) {
            return;
        }
        String v = classMode.trim().toLowerCase();
        if ("offline".equals(v) || "both".equals(v)) {
            ThrowUtils.throwIf(isBlank(city), ErrorCode.PARAMS_ERROR, "线下授课必须选择城市");
            ThrowUtils.throwIf(isBlank(address), ErrorCode.PARAMS_ERROR, "线下授课必须填写授课地址");
        }
    }

    private static void validateBudgetRange(BigDecimal budgetMin, BigDecimal budgetMax) {
        if (budgetMin != null) {
            ThrowUtils.throwIf(budgetMin.compareTo(BigDecimal.ZERO) <= 0, ErrorCode.PARAMS_ERROR, "预算下限需大于 0");
        }
        if (budgetMax != null) {
            ThrowUtils.throwIf(budgetMax.compareTo(BigDecimal.ZERO) <= 0, ErrorCode.PARAMS_ERROR, "预算上限需大于 0");
        }
        if (budgetMin != null && budgetMax != null) {
            ThrowUtils.throwIf(budgetMin.compareTo(budgetMax) > 0, ErrorCode.PARAMS_ERROR, "预算下限不能大于预算上限");
        }
    }

    private static String normalizeEducationRequirementForStorage(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        return v.isEmpty() ? null : v.toUpperCase();
    }

    private static String normalizeEducationRequirementForFilter(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        if ("UNLIMITED".equalsIgnoreCase(v)) return null;
        return v.toUpperCase();
    }

    private static String normalizePublisherIdentity(String raw) {
        if (raw == null) return null;
        return raw.trim().toUpperCase();
    }

    private static String resolvePublisherIdentityLabel(String raw) {
        PublisherIdentityEnum e = PublisherIdentityEnum.fromCode(raw);
        return e == null ? PublisherIdentityEnum.PARENT.getLabel() : e.getLabel();
    }

    private static String buildDisplayName(User user) {
        if (user == null) {
            return "用户";
        }
        String name = user.getName();
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        String phone = user.getPhone();
        if (phone != null && phone.length() >= 4) {
            return "用户" + phone.substring(phone.length() - 4);
        }
        return "用户";
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String firstNonBlank(String a, String b) {
        if (!isBlank(a)) return a;
        return isBlank(b) ? null : b;
    }

    private static CursorPageResponse<StudentJobPosting> buildCursorResponse(List<StudentJobPosting> list, Integer pageSize) {
        Long nextCursor = null;
        if (list != null && !list.isEmpty()) {
            nextCursor = list.get(list.size() - 1).getId();
        }
        boolean isLast = list == null || list.size() < pageSize;
        return new CursorPageResponse<>(nextCursor, isLast, list);
    }
}
