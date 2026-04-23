package com.ai.tutor.appointment.service.impl;

import com.ai.tutor.appointment.mapper.PositionPostMapper;
import com.ai.tutor.appointment.mapper.StudentJobPostingMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.config.TestBackdoorTeacherProperties;
import com.ai.tutor.appointment.mapper.OrganizationProfileMapper;
import com.ai.tutor.appointment.enums.PublisherIdentityEnum;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.job.CreateStudentJobPostingRequest;
import com.ai.tutor.appointment.model.dto.job.UpdateStudentJobPostingRequest;
import com.ai.tutor.appointment.model.entity.OrganizationProfile;
import com.ai.tutor.appointment.model.entity.StudentJobPosting;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.entity.PositionPost;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.DemandViewVO;
import com.ai.tutor.appointment.service.StudentJobPostingService;
import com.ai.tutor.appointment.service.TestBackdoorSeedService;
import com.ai.tutor.appointment.utils.CityCatalog;
import com.ai.tutor.common.metrics.BizKpiMetrics;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class StudentJobPostingServiceImpl implements StudentJobPostingService {

    @Resource
    private StudentJobPostingMapper studentJobPostingMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private OrganizationProfileMapper organizationProfileMapper;

    @Resource
    private PositionPostMapper positionPostMapper;
    @Resource
    private TestBackdoorTeacherProperties testBackdoorTeacherProperties;
    @Resource
    private TestBackdoorSeedService testBackdoorSeedService;
    @Resource
    private BizKpiMetrics bizKpiMetrics;

    private static final long TEST_EXCLUSIVE_DEMAND_ID = 666601L;

    @Override
    public Long create(CreateStudentJobPostingRequest request, Long uid) {
        ThrowUtils.throwIf(request == null || uid == null, ErrorCode.PARAMS_ERROR);
        validateCreate(request);

        String gradeCode = normalizeGradeCodeForStorage(request.getGradeCode());
        if (gradeCode == null) {
            gradeCode = deriveGradeCodeFromStageCode(request.getStageCode());
        }
        String stageCode = firstNonBlank(request.getStageCode(), deriveStageCodeFromGradeCode(gradeCode));

        Long subjectId = request.getSubjectId();
        String subjectName = trimToNull(request.getSubjectName());
        boolean subjectOther = Boolean.TRUE.equals(request.getSubjectOther());
        if (subjectName == null && subjectId != null) {
            List<PositionPost> posts = positionPostMapper.selectByIds(Collections.singletonList(subjectId));
            if (posts != null && !posts.isEmpty()) {
                subjectName = trimToNull(posts.get(0).getName());
            }
        }

        String normalizedClassMode = normalizeClassModeForStorage(request.getClassMode());
        String normalizedCity = CityCatalog.normalizeCityForStorage(normalizedClassMode, request.getCity());

        StudentJobPosting posting = StudentJobPosting.builder()
                .parentId(uid)
                .subjectId(subjectId)
                .subjectName(subjectName)
                .subjectIsOther(subjectOther ? 1 : 0)
                .title(request.getTitle())
                .description(trimToNull(request.getDescription()))
                .studentGender(normalizeStudentGenderForStorage(request.getStudentGender()))
                .gradeCode(gradeCode)
                .availableTime(trimToNull(request.getAvailableTime()))
                .teacherGenderPreference(normalizeTeacherGenderPreferenceForStorage(request.getTeacherGenderPreference()))
                .teacherRequirementDetail(trimToNull(request.getTeacherRequirementDetail()))
                .childAge(request.getChildAge())
                .classMode(normalizedClassMode)
                .city(normalizedCity)
                .address(request.getAddress())
                .frequencyPerWeek(request.getFrequencyPerWeek())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .stageCode(stageCode)
                .educationRequirement(normalizeEducationRequirementForStorage(request.getEducationRequirement()))
                .publisherIdentity(normalizePublisherIdentity(request.getPublisherIdentity()))
                .schedule(request.getSchedule())
                .bizStatus(1)
                .status(1)
                .build();
        int inserted = studentJobPostingMapper.insert(posting);
        ThrowUtils.throwIf(inserted <= 0 || posting.getId() == null, ErrorCode.OPERATION_ERROR);
        if (bizKpiMetrics != null) {
            /*
             * 中文注释：需求发布指标只在需求贴真实落库成功后累计，避免参数校验失败或重试未入库时污染供给口径。
             */
            bizKpiMetrics.incJobPostCreated(resolvePublisherRole(posting.getPublisherIdentity()));
        }
        return posting.getId();
    }

    @Override
    public void update(Long id, UpdateStudentJobPostingRequest request, Long uid) {
        ThrowUtils.throwIf(id == null || request == null || uid == null, ErrorCode.PARAMS_ERROR);

        StudentJobPosting db = studentJobPostingMapper.selectById(id);
        ThrowUtils.throwIf(db == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!uid.equals(db.getParentId()), ErrorCode.NO_AUTH_ERROR);
        boolean closingPosting = isManualClose(db, request);

        validateUpdate(request, db);

        String gradeCode = normalizeGradeCodeForStorage(request.getGradeCode());
        String stageCode = request.getStageCode();
        if (gradeCode == null) {
            gradeCode = null;
        }
        if (stageCode == null && gradeCode != null) {
            stageCode = deriveStageCodeFromGradeCode(gradeCode);
        }

        // 授课形式会直接决定后续沟通、课程与支付规则，因此需求创建后不允许修改。
        ThrowUtils.throwIf(request.getClassMode() != null, ErrorCode.PARAMS_ERROR, "授课形式创建后不可修改");

        String effectiveClassMode = db.getClassMode();
        String city = request.getCity() == null ? db.getCity() : request.getCity();
        String normalizedCity = CityCatalog.normalizeCityForStorage(effectiveClassMode, city);

        boolean touchSubject = request.getSubjectId() != null || request.getSubjectName() != null || request.getSubjectOther() != null;
        Long subjectId = request.getSubjectId();
        String subjectName = request.getSubjectName();
        boolean subjectOther = request.getSubjectOther() != null ? request.getSubjectOther() : (db.getSubjectIsOther() != null && db.getSubjectIsOther() == 1);
        if (!touchSubject) {
            subjectId = null;
            subjectName = null;
        } else {
            boolean clearSubject = request.getSubjectOther() != null && !request.getSubjectOther() && request.getSubjectName() == null;
            if (clearSubject) {
                subjectId = null;
                subjectName = null;
                subjectOther = false;
            }
            if (subjectName == null) {
                subjectName = db.getSubjectName();
            }
            subjectName = trimToNull(subjectName);
            if (subjectId == null && !clearSubject) {
                subjectId = db.getSubjectId();
            }
            if (subjectName == null && subjectId != null) {
                List<PositionPost> posts = positionPostMapper.selectByIds(Collections.singletonList(subjectId));
                if (posts != null && !posts.isEmpty()) {
                    subjectName = trimToNull(posts.get(0).getName());
                }
            }
        }

        StudentJobPosting toUpdate = StudentJobPosting.builder()
                .id(id)
                .subjectId(subjectId)
                .subjectName(subjectName)
                .subjectIsOther(touchSubject ? (subjectOther ? 1 : 0) : null)
                .title(request.getTitle())
                .description(request.getDescription())
                .studentGender(request.getStudentGender() == null ? null : normalizeStudentGenderForStorage(request.getStudentGender()))
                .gradeCode(gradeCode)
                .availableTime(request.getAvailableTime() == null ? null : trimToNull(request.getAvailableTime()))
                .teacherGenderPreference(request.getTeacherGenderPreference() == null ? null : normalizeTeacherGenderPreferenceForStorage(request.getTeacherGenderPreference()))
                .teacherRequirementDetail(request.getTeacherRequirementDetail() == null ? null : trimToNull(request.getTeacherRequirementDetail()))
                .childAge(request.getChildAge())
                .classMode(null)
                .city(request.getCity() == null ? null : normalizedCity)
                .address(request.getAddress())
                .frequencyPerWeek(request.getFrequencyPerWeek())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .stageCode(stageCode)
                .educationRequirement(request.getEducationRequirement() == null ? null : normalizeEducationRequirementForStorage(request.getEducationRequirement()))
                .publisherIdentity(request.getPublisherIdentity() == null ? null : normalizePublisherIdentity(request.getPublisherIdentity()))
                .schedule(request.getSchedule())
                .bizStatus(resolveBizStatusForStatusUpdate(request.getStatus(), db))
                .status(request.getStatus())
                .subjectTouched(touchSubject)
                .build();
        int updated = studentJobPostingMapper.updateById(toUpdate);
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR);
        if (bizKpiMetrics != null && closingPosting) {
            /*
             * 中文注释：需求关闭只在“发布中 -> 已关闭”首次状态迁移成功后计数，手动编辑或重复关闭不重复累计。
             */
            bizKpiMetrics.incJobPostClosed(resolvePublisherRole(db.getPublisherIdentity()), "cancelled");
        }
    }

    @Override
    public StudentJobPosting getById(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        StudentJobPosting posting = studentJobPostingMapper.selectById(id);
        ThrowUtils.throwIf(posting == null, ErrorCode.NOT_FOUND_ERROR);
        return posting;
    }

    @Override
    public DemandViewVO getViewById(Long id, Long viewerUid) {
        StudentJobPosting posting = getById(id);
        User user = userMapper.selectById(posting.getParentId());
        OrganizationProfile orgProfile = null;
        if (PublisherIdentityEnum.ORGANIZATION == PublisherIdentityEnum.fromCode(posting.getPublisherIdentity())) {
            orgProfile = organizationProfileMapper.selectByUserId(posting.getParentId());
        }
        DemandViewVO.Publisher publisher = DemandViewVO.Publisher.builder()
                .uid(posting.getParentId())
                .displayName(buildPublisherDisplayName(user, orgProfile))
                .avatar(user == null ? null : user.getAvatar())
                .identityLabel(resolvePublisherIdentityLabel(posting.getPublisherIdentity()))
                .build();

        DemandViewVO view = DemandViewVO.builder()
                .id(posting.getId())
                .parentId(posting.getParentId())
                .subjectId(posting.getSubjectId())
                .subjectName(posting.getSubjectName())
                .subjectIsOther(posting.getSubjectIsOther())
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
                .bizStatus(posting.getBizStatus())
                .status(posting.getStatus())
                .createTime(posting.getCreateTime())
                .updateTime(posting.getUpdateTime())
                .publisher(publisher)
                .build();
        if (bizKpiMetrics != null) {
            /*
             * 中文注释：需求详情浏览只在后端成功返回有效详情后计数，404 或无权限请求不会进入这个打点口径。
             */
            bizKpiMetrics.incJobDetailView(resolveViewerRole(viewerUid));
        }
        return view;
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
                                                              String subjectName,
                                                              Boolean subjectOther,
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
                                                              Long viewerUid,
                                                              CursorPageRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Integer pageSize = request.getPageSize();

        String edu = normalizeEducationRequirementForFilter(educationRequirement);
        String teacherGender = normalizeTeacherGenderPreferenceForFilter(teacherGenderPreference);
        String effectiveCity = CityCatalog.normalizeCityForFilter(city);
        List<StudentJobPosting> list = studentJobPostingMapper.listPublishedFiltered(
                subjectId,
                trimToNull(subjectName),
                subjectOther,
                effectiveCity,
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
        list = mergeExclusiveTestDemandForViewer(list, viewerUid, request);
        return buildCursorResponse(list, pageSize);
    }

    private List<StudentJobPosting> mergeExclusiveTestDemandForViewer(List<StudentJobPosting> list, Long viewerUid, CursorPageRequest request) {
        if (!shouldShowExclusiveTestDemand(viewerUid, request)) {
            return list;
        }
        if (testBackdoorSeedService != null) {
            testBackdoorSeedService.ensureSeed();
        }
        StudentJobPosting exclusive = studentJobPostingMapper.selectByIdVisibleForTestTeacher(TEST_EXCLUSIVE_DEMAND_ID);
        if (exclusive == null) {
            return list;
        }
        java.util.List<StudentJobPosting> merged = new java.util.ArrayList<>();
        merged.add(exclusive);
        if (list != null) {
            for (StudentJobPosting item : list) {
                if (item == null || TEST_EXCLUSIVE_DEMAND_ID == (item.getId() == null ? -1L : item.getId())) {
                    continue;
                }
                merged.add(item);
            }
        }
        Integer pageSize = request == null ? null : request.getPageSize();
        if (pageSize != null && pageSize > 0 && merged.size() > pageSize) {
            return new java.util.ArrayList<>(merged.subList(0, pageSize));
        }
        return merged;
    }

    private boolean shouldShowExclusiveTestDemand(Long viewerUid, CursorPageRequest request) {
        if (testBackdoorTeacherProperties == null || !testBackdoorTeacherProperties.isEnabled()) {
            return false;
        }
        if (viewerUid == null || !viewerUid.equals(testBackdoorTeacherProperties.getUserId())) {
            return false;
        }
        return request == null || request.getCursor() == null;
    }

    private static final Set<String> CLASS_MODES = Set.of("online", "offline");
    private static final Set<String> GENDER_CODES = Set.of("male", "female", "both");

    private static void validateCreate(CreateStudentJobPostingRequest request) {
        String subjectName = request.getSubjectName();
        boolean subjectOther = Boolean.TRUE.equals(request.getSubjectOther());
        ThrowUtils.throwIf(isBlank(subjectName), ErrorCode.PARAMS_ERROR, "教学科目不能为空");
        ThrowUtils.throwIf(subjectOther && isBlank(subjectName), ErrorCode.PARAMS_ERROR, "请选择其他时需填写科目");
        ThrowUtils.throwIf(isBlank(request.getTitle()), ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(isBlank(request.getDescription()), ErrorCode.PARAMS_ERROR, "学生情况描述不能为空");
        ThrowUtils.throwIf(request.getDescription() != null && request.getDescription().trim().length() < 10, ErrorCode.PARAMS_ERROR, "学生情况描述至少10个字");
        ThrowUtils.throwIf(isBlank(request.getStudentGender()), ErrorCode.PARAMS_ERROR, "学员性别不能为空");
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
        ThrowUtils.throwIf(isBlank(request.getTeacherRequirementDetail()), ErrorCode.PARAMS_ERROR, "对教员的详细要求不能为空");
        ThrowUtils.throwIf(request.getTeacherRequirementDetail() != null && request.getTeacherRequirementDetail().trim().length() < 10, ErrorCode.PARAMS_ERROR, "对教员的详细要求至少10个字");
        if (!isBlank(request.getTeacherGenderPreference())) {
            ThrowUtils.throwIf(!GENDER_CODES.contains(request.getTeacherGenderPreference().trim().toLowerCase()), ErrorCode.PARAMS_ERROR, "教师性别偏好不合法");
        }
        ThrowUtils.throwIf(!Set.of("male", "female").contains(request.getStudentGender().trim().toLowerCase()), ErrorCode.PARAMS_ERROR, "学员性别不合法");
        ThrowUtils.throwIf(request.getBudgetMin() == null || request.getBudgetMax() == null, ErrorCode.PARAMS_ERROR, "预算不能为空");

        validateModeAddress(normalizeClassModeForStorage(request.getClassMode()), request.getCity(), request.getAddress());
        validateBudgetRange(request.getBudgetMin(), request.getBudgetMax());
    }

    private static void validateUpdate(UpdateStudentJobPostingRequest request, StudentJobPosting db) {
        String classMode = db.getClassMode();
        String title = firstNonBlank(request.getTitle(), db.getTitle());
        String description = firstNonBlank(request.getDescription(), db.getDescription());
        String teacherRequirementDetail = firstNonBlank(request.getTeacherRequirementDetail(), db.getTeacherRequirementDetail());
        Boolean subjectOther = request.getSubjectOther() == null ? (db.getSubjectIsOther() != null && db.getSubjectIsOther() == 1) : request.getSubjectOther();
        String subjectName = request.getSubjectName() == null ? db.getSubjectName() : request.getSubjectName();
        String studentGender = firstNonBlank(request.getStudentGender(), db.getStudentGender());
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

        ThrowUtils.throwIf(isBlank(subjectName), ErrorCode.PARAMS_ERROR, "教学科目不能为空");
        ThrowUtils.throwIf(Boolean.TRUE.equals(subjectOther) && isBlank(subjectName), ErrorCode.PARAMS_ERROR, "请选择其他时需填写科目");
        ThrowUtils.throwIf(isBlank(title), ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(isBlank(description), ErrorCode.PARAMS_ERROR, "学生情况描述不能为空");
        ThrowUtils.throwIf(description != null && description.trim().length() < 10, ErrorCode.PARAMS_ERROR, "学生情况描述至少10个字");
        ThrowUtils.throwIf(isBlank(studentGender), ErrorCode.PARAMS_ERROR, "学员性别不能为空");
        ThrowUtils.throwIf(isBlank(classMode), ErrorCode.PARAMS_ERROR, "授课方式不能为空");
        ThrowUtils.throwIf(request.getClassMode() != null, ErrorCode.PARAMS_ERROR, "授课形式创建后不可修改");
        ThrowUtils.throwIf(freq == null || freq < 1 || freq > 7, ErrorCode.PARAMS_ERROR, "授课频次需在 1~7 之间");
        ThrowUtils.throwIf(isBlank(gradeCode), ErrorCode.PARAMS_ERROR, "学生年级不能为空");
        ThrowUtils.throwIf(isBlank(stage), ErrorCode.PARAMS_ERROR, "授课学段不能为空");
        ThrowUtils.throwIf(isBlank(edu), ErrorCode.PARAMS_ERROR, "学历要求不能为空");
        ThrowUtils.throwIf(isBlank(pubId), ErrorCode.PARAMS_ERROR, "发布者身份不能为空");
        ThrowUtils.throwIf(PublisherIdentityEnum.fromCode(pubId) == null, ErrorCode.PARAMS_ERROR, "发布者身份不合法");
        ThrowUtils.throwIf(isBlank(teacherRequirementDetail), ErrorCode.PARAMS_ERROR, "对教员的详细要求不能为空");
        ThrowUtils.throwIf(teacherRequirementDetail != null && teacherRequirementDetail.trim().length() < 10, ErrorCode.PARAMS_ERROR, "对教员的详细要求至少10个字");
        if (!isBlank(request.getTeacherGenderPreference())) {
            ThrowUtils.throwIf(!GENDER_CODES.contains(request.getTeacherGenderPreference().trim().toLowerCase()), ErrorCode.PARAMS_ERROR, "教师性别偏好不合法");
        }
        ThrowUtils.throwIf(!Set.of("male", "female").contains(studentGender.trim().toLowerCase()), ErrorCode.PARAMS_ERROR, "学员性别不合法");
        ThrowUtils.throwIf(budgetMin == null || budgetMax == null, ErrorCode.PARAMS_ERROR, "预算不能为空");

        validateModeAddress(classMode, city, address);
        validateBudgetRange(budgetMin, budgetMax);
    }

    private static void validateModeAddress(String classMode, String city, String address) {
        if (classMode == null) {
            return;
        }
        String v = classMode.trim().toLowerCase();
        if ("offline".equals(v)) {
            ThrowUtils.throwIf(isBlank(city), ErrorCode.PARAMS_ERROR, "线下授课必须选择城市");
            ThrowUtils.throwIf(isBlank(address), ErrorCode.PARAMS_ERROR, "线下授课必须填写授课地址");
        }
    }

    private static String normalizeClassModeForStorage(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toLowerCase();
        ThrowUtils.throwIf(!CLASS_MODES.contains(v), ErrorCode.PARAMS_ERROR, "授课方式仅支持线上或线下");
        return v;
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

    /**
     * 发布状态与业务状态要保持一致：
     * - 手动关闭需求时，落为“已关闭”
     * - 从已关闭重新公开时，回到“匹配中”，重新进入公开推荐池
     * - 其他更新不覆盖沟通/合作中的真实业务状态
     */
    private static Integer resolveBizStatusForStatusUpdate(Integer nextStatus, StudentJobPosting db) {
        if (nextStatus == null || db == null) {
            return null;
        }
        if (nextStatus == 0) {
            return 6;
        }
        if (nextStatus == 1 && (db.getStatus() == null || db.getStatus() == 0 || Integer.valueOf(6).equals(db.getBizStatus()))) {
            return 1;
        }
        return null;
    }

    private static boolean isManualClose(StudentJobPosting db, UpdateStudentJobPostingRequest request) {
        if (db == null || request == null || request.getStatus() == null) {
            return false;
        }
        return Integer.valueOf(1).equals(db.getStatus()) && Integer.valueOf(0).equals(request.getStatus());
    }

    private static String resolvePublisherRole(String publisherIdentity) {
        PublisherIdentityEnum identity = PublisherIdentityEnum.fromCode(publisherIdentity);
        return identity == PublisherIdentityEnum.ORGANIZATION ? "org" : "student";
    }

    private String resolveViewerRole(Long viewerUid) {
        if (viewerUid == null) {
            return "unknown";
        }
        User viewer = userMapper.selectById(viewerUid);
        if (viewer == null || viewer.getUserType() == null) {
            return "unknown";
        }
        if (viewer.getUserType() == 1) {
            return "teacher";
        }
        if (viewer.getUserType() == 2) {
            return "student";
        }
        if (viewer.getUserType() == 3) {
            return "org";
        }
        return "unknown";
    }

    private static String normalizePublisherIdentity(String raw) {
        if (raw == null) return null;
        return raw.trim().toUpperCase();
    }

    private static String resolvePublisherIdentityLabel(String raw) {
        PublisherIdentityEnum e = PublisherIdentityEnum.fromCode(raw);
        return e == null ? PublisherIdentityEnum.PARENT.getLabel() : e.getLabel();
    }

    private static String buildPublisherDisplayName(User user, OrganizationProfile orgProfile) {
        if (orgProfile != null) {
            String orgName = orgProfile.getOrgName();
            if (orgName != null && !orgName.trim().isEmpty()) {
                return orgName.trim();
            }
        }
        return buildDisplayName(user);
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
