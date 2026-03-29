package com.ai.tutor.admin.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.ai.tutor.admin.mapper.AdminOrganizationManageMapper;
import com.ai.tutor.admin.mapper.OrganizationAccountMapper;
import com.ai.tutor.admin.mapper.OrganizationProfileMapper;
import com.ai.tutor.admin.mapper.StudentProfileMapper;
import com.ai.tutor.admin.mapper.UserMapper;
import com.ai.tutor.admin.model.dto.AdminOrganizationCreateRequest;
import com.ai.tutor.admin.model.dto.AdminOrganizationUpdateRequest;
import com.ai.tutor.admin.model.entity.OrganizationAccount;
import com.ai.tutor.admin.model.entity.OrganizationProfile;
import com.ai.tutor.admin.model.entity.StudentProfile;
import com.ai.tutor.admin.model.entity.User;
import com.ai.tutor.admin.model.vo.AdminOrganizationCreateResponse;
import com.ai.tutor.admin.model.vo.AdminOrganizationDetailVO;
import com.ai.tutor.admin.model.vo.AdminOrganizationRowVO;
import com.ai.tutor.admin.model.vo.PageResult;
import com.ai.tutor.admin.service.AdminOrganizationService;
import com.ai.tutor.common.metrics.BizKpiMetrics;
import com.ai.tutor.enums.ErrorCode;
import com.ai.tutor.utils.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AdminOrganizationServiceImpl implements AdminOrganizationService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    @Resource
    private UserMapper userMapper;

    @Resource
    private StudentProfileMapper studentProfileMapper;

    @Resource
    private OrganizationProfileMapper organizationProfileMapper;

    @Resource
    private OrganizationAccountMapper organizationAccountMapper;

    @Resource
    private AdminOrganizationManageMapper adminOrganizationManageMapper;

    @Resource
    private BizKpiMetrics bizKpiMetrics;

    @Override
    @Transactional
    public AdminOrganizationCreateResponse create(AdminOrganizationCreateRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        String orgName = trimToNull(request.getOrgName());
        String contactPhone = normalizePhone(request.getContactPhone());
        ThrowUtils.throwIf(orgName == null, ErrorCode.PARAMS_ERROR, "机构名称不能为空");
        ThrowUtils.throwIf(contactPhone == null || !PHONE_PATTERN.matcher(contactPhone).matches(), ErrorCode.PARAMS_ERROR, "联系人手机号格式不正确");

        User exist = userMapper.selectByPhone(contactPhone);
        ThrowUtils.throwIf(exist != null, ErrorCode.OPERATION_ERROR, "手机号已存在");

        String username = trimToNull(request.getUsername());
        if (username == null) {
            username = genUsername(orgName);
        }
        ThrowUtils.throwIf(organizationAccountMapper.selectByUsername(username) != null, ErrorCode.OPERATION_ERROR, "登录账号已存在");

        String initialPassword = trimToNull(request.getInitialPassword());
        if (initialPassword == null) {
            initialPassword = genPassword();
        }
        ThrowUtils.throwIf(initialPassword.length() < 8, ErrorCode.PARAMS_ERROR, "初始密码至少 8 位");

        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
                .name(orgName)
                .phone(contactPhone)
                .avatar(null)
                .sex(null)
                .openId(null)
                .activeStatus(2)
                .lastOptTime(now)
                .ipInfo(null)
                .itemId(null)
                .status(0)
                .userType(3)
                .refId(null)
                .createTime(now)
                .updateTime(now)
                .build();
        userMapper.insert(user);
        ThrowUtils.throwIf(user.getId() == null, ErrorCode.SYSTEM_ERROR, "创建机构用户失败");
        if (bizKpiMetrics != null) {
            /*
             * Grafana 业务 KPI 指标打点（每日新注册用户数：机构）。
             * - metric: ai_tutor_biz_user_register_total
             * - labels: role=org
             * - PromQL（按天）：sum by (role) (increase(ai_tutor_biz_user_register_total[1d]))
             *
             * 说明：机构账号由管理端创建，本打点仅在创建成功后计数。
             */
            bizKpiMetrics.incUserRegister("org");
        }

        StudentProfile sp = StudentProfile.builder()
                .userId(user.getId())
                .realName(orgName)
                .age(null)
                .address(trimToNull(request.getAddress()))
                .demandDescription(trimToNull(request.getIntro()))
                .budget(null)
                .status(1)
                .createTime(now)
                .updateTime(now)
                .build();
        studentProfileMapper.insert(sp);
        ThrowUtils.throwIf(sp.getId() == null, ErrorCode.SYSTEM_ERROR, "初始化机构资料失败");
        userMapper.updateRefId(user.getId(), sp.getId());

        OrganizationProfile profile = OrganizationProfile.builder()
                .userId(user.getId())
                .orgName(orgName)
                .intro(trimToNull(request.getIntro()))
                .contactName(trimToNull(request.getContactName()))
                .contactPhone(contactPhone)
                .address(trimToNull(request.getAddress()))
                .licenseNo(trimToNull(request.getLicenseNo()))
                .splitPlatformPercent(request.getSplitPlatformPercent() == null ? 50 : request.getSplitPlatformPercent())
                .splitOrgPercent(request.getSplitOrgPercent() == null ? 50 : request.getSplitOrgPercent())
                .status(1)
                .createTime(now)
                .updateTime(now)
                .build();
        organizationProfileMapper.insert(profile);

        String hash = BCrypt.hashpw(initialPassword, BCrypt.gensalt());
        OrganizationAccount account = OrganizationAccount.builder()
                .orgUserId(user.getId())
                .username(username)
                .passwordHash(hash)
                .mustChangePassword(1)
                .status(1)
                .lastLoginTime(null)
                .createTime(now)
                .updateTime(now)
                .build();
        organizationAccountMapper.insert(account);

        return AdminOrganizationCreateResponse.builder()
                .orgUserId(user.getId())
                .username(username)
                .initialPassword(initialPassword)
                .build();
    }

    @Override
    public PageResult<AdminOrganizationRowVO> page(String q, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        long offset = (long) (safePage - 1) * safeSize;
        String keyword = trimToNull(q);
        List<AdminOrganizationRowVO> records = adminOrganizationManageMapper.page(keyword, offset, safeSize);
        long total = adminOrganizationManageMapper.count(keyword);
        return PageResult.<AdminOrganizationRowVO>builder()
                .records(records)
                .total(total)
                .size(safeSize)
                .current(safePage)
                .build();
    }

    @Override
    public AdminOrganizationDetailVO getDetail(Long orgUserId) {
        ThrowUtils.throwIf(orgUserId == null, ErrorCode.PARAMS_ERROR);
        AdminOrganizationDetailVO vo = adminOrganizationManageMapper.selectDetail(orgUserId);
        ThrowUtils.throwIf(vo == null, ErrorCode.NOT_FOUND_ERROR, "机构不存在");
        return vo;
    }

    @Override
    @Transactional
    public void update(Long orgUserId, AdminOrganizationUpdateRequest request) {
        ThrowUtils.throwIf(orgUserId == null || request == null, ErrorCode.PARAMS_ERROR);
        AdminOrganizationDetailVO current = adminOrganizationManageMapper.selectDetail(orgUserId);
        ThrowUtils.throwIf(current == null, ErrorCode.NOT_FOUND_ERROR, "机构不存在");

        String orgName = trimToNull(request.getOrgName());
        String username = trimToNull(request.getUsername());
        String initialPassword = trimToNull(request.getInitialPassword());
        Integer accountStatus = request.getAccountStatus();
        String contactPhone = normalizePhone(request.getContactPhone());
        String contactName = trimToNull(request.getContactName());
        String address = trimToNull(request.getAddress());
        String intro = trimToNull(request.getIntro());
        String licenseNo = trimToNull(request.getLicenseNo());
        Integer splitPlatformPercent = request.getSplitPlatformPercent();
        Integer splitOrgPercent = request.getSplitOrgPercent();

        if (username != null && current.getUsername() != null && username.equals(current.getUsername())) {
            username = null;
        }
        if (username != null) {
            Long existOrgUserId = adminOrganizationManageMapper.selectOrgUserIdByUsername(username);
            ThrowUtils.throwIf(existOrgUserId != null && !existOrgUserId.equals(orgUserId), ErrorCode.OPERATION_ERROR, "登录账号已存在");
        }

        if (contactPhone != null && current.getContactPhone() != null && contactPhone.equals(current.getContactPhone())) {
            contactPhone = null;
        }
        if (contactPhone != null) {
            User exist = userMapper.selectByPhone(contactPhone);
            ThrowUtils.throwIf(exist != null && !exist.getId().equals(orgUserId), ErrorCode.OPERATION_ERROR, "手机号已存在");
        }

        if (initialPassword != null) {
            ThrowUtils.throwIf(initialPassword.length() < 8, ErrorCode.PARAMS_ERROR, "初始密码至少 8 位");
        }
        if (accountStatus != null) {
            ThrowUtils.throwIf(accountStatus != 0 && accountStatus != 1, ErrorCode.PARAMS_ERROR, "账号状态不合法");
        }

        if (orgName != null || contactPhone != null) {
            adminOrganizationManageMapper.updateOrgUserBase(orgUserId, orgName, contactPhone);
        }

        adminOrganizationManageMapper.updateOrgProfile(
                orgUserId,
                orgName,
                intro,
                contactName,
                contactPhone,
                address,
                licenseNo,
                splitPlatformPercent,
                splitOrgPercent
        );

        if (username != null || accountStatus != null) {
            adminOrganizationManageMapper.updateOrgAccount(orgUserId, username, accountStatus);
        }

        if (initialPassword != null) {
            String hash = BCrypt.hashpw(initialPassword, BCrypt.gensalt());
            adminOrganizationManageMapper.resetOrgAccountPassword(orgUserId, hash, 1);
        }
    }

    @Override
    @Transactional
    public void disable(Long orgUserId) {
        ThrowUtils.throwIf(orgUserId == null, ErrorCode.PARAMS_ERROR);
        AdminOrganizationDetailVO current = adminOrganizationManageMapper.selectDetail(orgUserId);
        ThrowUtils.throwIf(current == null, ErrorCode.NOT_FOUND_ERROR, "机构不存在");
        adminOrganizationManageMapper.disableOrgAccount(orgUserId);
        adminOrganizationManageMapper.disableOrgProfile(orgUserId);
        adminOrganizationManageMapper.disableStudentProfile(orgUserId);
        adminOrganizationManageMapper.blacklistOrgUser(orgUserId);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizePhone(String s) {
        String raw = trimToNull(s);
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    private static String genUsername(String orgName) {
        String base = orgName.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_\\-]", "");
        if (base.length() > 12) {
            base = base.substring(0, 12);
        }
        String suffix = String.valueOf(System.currentTimeMillis() % 1000000);
        return (base.isEmpty() ? "org" : base) + "_" + suffix;
    }

    private static String genPassword() {
        String suffix = String.valueOf(System.currentTimeMillis() % 1000000);
        return "Org@" + suffix + "a";
    }
}
