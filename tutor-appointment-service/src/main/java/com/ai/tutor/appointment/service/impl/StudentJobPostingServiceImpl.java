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

        String gradeCode = normalizeGradeCodeForStorage(request.getGradeCode());
        if (gradeCode == null) {
            gradeCode = deriveGradeCodeFromStageCode(request.getStageCode());
        }
        String stageCode = firstNonBlank(request.getStageCode(), deriveStageCodeFromGradeCode(gradeCode));

        StudentJobPosting posting = StudentJobPosting.builder()
                .parentId(uid)
                .subjectId(request.getSubjectId())
                .title(request.getTitle())
                .description(request.getDescription())
                .studentGender(normalizeStudentGenderForStorage(request.getStudentGender()))
                .gradeCode(gradeCode)
                .availableTime(trimToNull(request.getAvailableTime()))
                .teacherGenderPreference(normalizeTeacherGenderPreferenceForStorage(request.getTeacherGenderPreference()))
                .teacherRequirementDetail(trimToNull(request.getTeacherRequirementDetail()))
                .childAge(request.getChildAge())
                .classMode(request.getClassMode())
                .city(request.getCity())
                .address(request.getAddress())
                .frequencyPerWeek(request.getFrequencyPerWeek())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .stageCode(stageCode)
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

        String gradeCode = normalizeGradeCodeForStorage(request.getGradeCode());
        String stageCode = request.getStageCode();
        if (gradeCode == null) {
            gradeCode = null;
        }
        if (stageCode == null && gradeCode != null) {
            stageCode = deriveStageCodeFromGradeCode(gradeCode);
        }

        StudentJobPosting toUpdate = StudentJobPosting.builder()
                .id(id)
                .subjectId(request.getSubjectId())
                .title(request.getTitle())
                .description(request.getDescription())
                .studentGender(request.getStudentGender() == null ? null : normalizeStudentGenderForStorage(request.getStudentGender()))
                .gradeCode(gradeCode)
                .availableTime(request.getAvailableTime() == null ? null : trimToNull(request.getAvailableTime()))
                .teacherGenderPreference(request.getTeacherGenderPreference() == null ? null : normalizeTeacherGenderPreferenceForStorage(request.getTeacherGenderPreference()))
                .teacherRequirementDetail(request.getTeacherRequirementDetail() == null ? null : trimToNull(request.getTeacherRequirementDetail()))
                .childAge(request.getChildAge())
                .classMode(request.getClassMode())
                .city(request.getCity())
                .address(request.getAddress())
                .frequencyPerWeek(request.getFrequencyPerWeek())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .stageCode(stageCode)
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
                .studentGender(posting.getStudentGender())
                .gradeCode(posting.getGradeCode())
                .availableTime(posting.getAvailableTime())
                .teacherGenderPreference(posting.getTeacherGenderPreference())
                .teacherRequirementDetail(posting.getTeacherRequirementDetail())
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
                                                              String teacherGenderPreference,
                                                              BigDecimal budgetMin,
                                                              BigDecimal budgetMax,
                                                              String keyword,
                                                              String sort,
                                                              CursorPageRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Integer pageSize = request.getPageSize();

        String edu = normalizeEducationRequirementForFilter(educationRequirement);
        String teacherGender = normalizeTeacherGenderPreferenceForFilter(teacherGenderPreference);
        List<StudentJobPosting> list = studentJobPostingMapper.listPublishedFiltered(
                subjectId,
                city,
                classMode,
                stageCode,
                frequencyPerWeek,
                edu,
                teacherGender,
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
    private static final Set<String> GENDER_CODES = Set.of("male", "female", "both");

    private static void validateCreate(CreateStudentJobPostingRequest request) {
        ThrowUtils.throwIf(request.getSubjectId() == null, ErrorCode.PARAMS_ERROR, "科目不能为空");
        ThrowUtils.throwIf(isBlank(request.getTitle()), ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(isBlank(request.getClassMode()), ErrorCode.PARAMS_ERROR, "授课方式不能为空");
        ThrowUtils.throwIf(!CLASS_MODES.contains(request.getClassMode().trim().toLowerCase()), ErrorCode.PARAMS_ERROR, "授课方式不合法");
        ThrowUtils.throwIf(request.getFrequencyPerWeek() == null, ErrorCode.PARAMS_ERROR, "授课频次不能为空");
        ThrowUtils.throwIf(request.getFrequencyPerWeek() < 1 || request.getFrequencyPerWeek() > 7, ErrorCode.PARAMS_ERROR, "授课频次需在 1~7 之间");
        String gradeCode = normalizeGradeCodeForStorage(request.getGradeCode());
        if (gradeCode == null) {
            gradeCode = deriveGradeCodeFromStageCode(request.getStageCode());
        }
        ThrowUtils.throwIf(isBlank(gradeCode), ErrorCode.PARAMS_ERROR, "学生年级不能为空");
        String stageCode = firstNonBlank(request.getStageCode(), deriveStageCodeFromGradeCode(gradeCode));
        ThrowUtils.throwIf(isBlank(stageCode), ErrorCode.PARAMS_ERROR, "授课学段不能为空");
        ThrowUtils.throwIf(isBlank(request.getEducationRequirement()), ErrorCode.PARAMS_ERROR, "学历要求不能为空");
        ThrowUtils.throwIf(isBlank(request.getPublisherIdentity()), ErrorCode.PARAMS_ERROR, "发布者身份不能为空");
        ThrowUtils.throwIf(PublisherIdentityEnum.fromCode(request.getPublisherIdentity()) == null, ErrorCode.PARAMS_ERROR, "发布者身份不合法");
        if (!isBlank(request.getTeacherGenderPreference())) {
            ThrowUtils.throwIf(!GENDER_CODES.contains(request.getTeacherGenderPreference().trim().toLowerCase()), ErrorCode.PARAMS_ERROR, "教师性别偏好不合法");
        }
        if (!isBlank(request.getStudentGender())) {
            ThrowUtils.throwIf(!Set.of("male", "female").contains(request.getStudentGender().trim().toLowerCase()), ErrorCode.PARAMS_ERROR, "学员性别不合法");
        }

        validateModeAddress(request.getClassMode(), request.getCity(), request.getAddress());
        validateBudgetRange(request.getBudgetMin(), request.getBudgetMax());
    }

    private static void validateUpdate(UpdateStudentJobPostingRequest request, StudentJobPosting db) {
        String classMode = firstNonBlank(request.getClassMode(), db.getClassMode());
        String title = firstNonBlank(request.getTitle(), db.getTitle());
        Long subjectId = request.getSubjectId() == null ? db.getSubjectId() : request.getSubjectId();
        Integer freq = request.getFrequencyPerWeek() == null ? db.getFrequencyPerWeek() : request.getFrequencyPerWeek();
        String gradeCode = firstNonBlank(request.getGradeCode(), db.getGradeCode());
        if (isBlank(gradeCode)) {
            gradeCode = deriveGradeCodeFromStageCode(firstNonBlank(request.getStageCode(), db.getStageCode()));
        }
        String stage = firstNonBlank(request.getStageCode(), db.getStageCode());
        if (isBlank(stage)) {
            stage = deriveStageCodeFromGradeCode(gradeCode);
        }
        String edu = firstNonBlank(request.getEducationRequirement(), db.getEducationRequirement());
        String pubId = firstNonBlank(request.getPublisherIdentity(), db.getPublisherIdentity());
        String city = request.getCity() == null ? db.getCity() : request.getCity();
        String address = request.getAddress() == null ? db.getAddress() : request.getAddress();
        BigDecimal budgetMin = request.getBudgetMin() == null ? db.getBudgetMin() : request.getBudgetMin();
        BigDecimal budgetMax = request.getBudgetMax() == null ? db.getBudgetMax() : request.getBudgetMax();

        ThrowUtils.throwIf(subjectId == null, ErrorCode.PARAMS_ERROR, "科目不能为空");
        ThrowUtils.throwIf(isBlank(title), ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(isBlank(classMode), ErrorCode.PARAMS_ERROR, "授课方式不能为空");
        ThrowUtils.throwIf(!CLASS_MODES.contains(classMode.trim().toLowerCase()), ErrorCode.PARAMS_ERROR, "授课方式不合法");
        ThrowUtils.throwIf(freq == null || freq < 1 || freq > 7, ErrorCode.PARAMS_ERROR, "授课频次需在 1~7 之间");
        ThrowUtils.throwIf(isBlank(gradeCode), ErrorCode.PARAMS_ERROR, "学生年级不能为空");
        ThrowUtils.throwIf(isBlank(stage), ErrorCode.PARAMS_ERROR, "授课学段不能为空");
        ThrowUtils.throwIf(isBlank(edu), ErrorCode.PARAMS_ERROR, "学历要求不能为空");
        ThrowUtils.throwIf(isBlank(pubId), ErrorCode.PARAMS_ERROR, "发布者身份不能为空");
        ThrowUtils.throwIf(PublisherIdentityEnum.fromCode(pubId) == null, ErrorCode.PARAMS_ERROR, "发布者身份不合法");
        if (!isBlank(request.getTeacherGenderPreference())) {
            ThrowUtils.throwIf(!GENDER_CODES.contains(request.getTeacherGenderPreference().trim().toLowerCase()), ErrorCode.PARAMS_ERROR, "教师性别偏好不合法");
        }
        if (!isBlank(request.getStudentGender())) {
            ThrowUtils.throwIf(!Set.of("male", "female").contains(request.getStudentGender().trim().toLowerCase()), ErrorCode.PARAMS_ERROR, "学员性别不合法");
        }

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

    private static String normalizeTeacherGenderPreferenceForStorage(String raw) {
        if (raw == null) return "both";
        String v = raw.trim().toLowerCase();
        if (v.isEmpty()) return "both";
        ThrowUtils.throwIf(!GENDER_CODES.contains(v), ErrorCode.PARAMS_ERROR, "教师性别偏好不合法");
        return v;
    }

    private static String normalizeTeacherGenderPreferenceForFilter(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toLowerCase();
        if (v.isEmpty()) return null;
        ThrowUtils.throwIf(!GENDER_CODES.contains(v), ErrorCode.PARAMS_ERROR, "教师性别偏好不合法");
        return v;
    }

    private static String normalizeStudentGenderForStorage(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toLowerCase();
        if (v.isEmpty()) return null;
        ThrowUtils.throwIf(!Set.of("male", "female").contains(v), ErrorCode.PARAMS_ERROR, "学员性别不合法");
        return v;
    }

    private static String normalizeGradeCodeForStorage(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toUpperCase();
        return v.isEmpty() ? null : v;
    }

    private static String deriveStageCodeFromGradeCode(String gradeCode) {
        if (gradeCode == null) return null;
        String v = gradeCode.trim().toUpperCase();
        if (v.isEmpty()) return null;
        if ("PRESCHOOL".equals(v)) return "PRESCHOOL";
        if (v.startsWith("GRADE")) return "PRIMARY";
        if (v.startsWith("JUNIOR")) return "JUNIOR";
        if (v.startsWith("SENIOR")) return "SENIOR";
        return "OTHER";
    }

    private static String deriveGradeCodeFromStageCode(String stageCode) {
        if (stageCode == null) return null;
        String v = stageCode.trim().toUpperCase();
        if (v.isEmpty()) return null;
        if ("PRESCHOOL".equals(v)) return "PRESCHOOL";
        if ("PRIMARY".equals(v)) return "GRADE1";
        if ("JUNIOR".equals(v)) return "JUNIOR1";
        if ("SENIOR".equals(v)) return "SENIOR1";
        if ("OTHER".equals(v)) return "ADULT";
        return null;
    }

    private static String trimToNull(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        return v.isEmpty() ? null : v;
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
