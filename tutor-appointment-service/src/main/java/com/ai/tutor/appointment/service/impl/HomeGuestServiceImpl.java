package com.ai.tutor.appointment.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.ai.tutor.appointment.config.HomeGuestProperties;
import com.ai.tutor.appointment.mapper.*;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.home.HomeHotTutorAggRow;
import com.ai.tutor.appointment.model.entity.*;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.SubjectTreeNodeVO;
import com.ai.tutor.appointment.model.vo.home.HomeGuestVOs;
import com.ai.tutor.appointment.service.HomeGuestService;
import com.ai.tutor.appointment.service.SubjectQueryService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class HomeGuestServiceImpl implements HomeGuestService {

    private static final int DEFAULT_LIMIT = 10;

    @Resource
    private HomeGuestProperties homeGuestProperties;

    @Resource
    private SubjectQueryService subjectQueryService;

    @Resource
    private PositionPostMapper positionPostMapper;

    @Resource
    private TeacherJobPostingMapper teacherJobPostingMapper;

    @Resource
    private StudentJobPostingMapper studentJobPostingMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TeacherProfileMapper teacherProfileMapper;

    @Override
    public HomeGuestVOs.HomeConfigVO getHomeConfig(String city) {
        HomeGuestVOs.HomeConfigVO.Search search = new HomeGuestVOs.HomeConfigVO.Search(
                homeGuestProperties.getSearch().getPlaceholder(),
                homeGuestProperties.getSearch().getDefaultMode()
        );

        List<HomeGuestVOs.HomeConfigVO.NavItem> nav = homeGuestProperties.getNav()
                .stream()
                .map(n -> new HomeGuestVOs.HomeConfigVO.NavItem(n.getKey(), n.getName(), n.getLink()))
                .toList();

        HomeGuestVOs.HomeConfigVO.AuthEntry authEntry = new HomeGuestVOs.HomeConfigVO.AuthEntry(
                homeGuestProperties.getAuthEntry().getLoginText(),
                homeGuestProperties.getAuthEntry().getLink()
        );

        return new HomeGuestVOs.HomeConfigVO(
                defaultIfBlank(city, homeGuestProperties.getDefaultCity()),
                Boolean.TRUE.equals(homeGuestProperties.getCitySelectable()),
                search,
                nav,
                authEntry
        );
    }

    @Override
    public HomeGuestVOs.GeoLocateVO locate(HttpServletRequest request) {
        String ip = getClientIp(request);
        String city = homeGuestProperties.getDefaultCity();
        List<String> suggestCities = List.of(city, "上海", "广州", "深圳");
        return new HomeGuestVOs.GeoLocateVO(ip, city, null, null, suggestCities);
    }

    @Override
    public HomeGuestVOs.HotWordsVO getHotWords(String city, Integer limit) {
        int safeLimit = safeLimit(limit, DEFAULT_LIMIT, 50);
        String effectiveCity = defaultIfBlank(city, homeGuestProperties.getDefaultCity());

        List<HomeGuestVOs.HotWordsVO.HotWord> list = homeGuestProperties.getHotWords()
                .stream()
                .filter(w -> matchCity(w.getCities(), effectiveCity))
                .limit(safeLimit)
                .map(w -> new HomeGuestVOs.HotWordsVO.HotWord(w.getWord(), defaultIfBlank(w.getType(), "keyword")))
                .toList();

        return new HomeGuestVOs.HotWordsVO(LocalDateTime.now(), list);
    }

    @Override
    public HomeGuestVOs.SearchSuggestVO suggest(String q, String city, Integer limit) {
        String keyword = StrUtil.trimToEmpty(q);
        if (keyword.isBlank()) {
            return new HomeGuestVOs.SearchSuggestVO(q, List.of());
        }
        int safeLimit = safeLimit(limit, DEFAULT_LIMIT, 20);

        int subjectLimit = Math.min(5, safeLimit);
        int remain = safeLimit - subjectLimit;
        int serviceLimit = Math.max(0, remain / 2);
        int demandLimit = Math.max(0, remain - serviceLimit);

        List<HomeGuestVOs.SearchSuggestVO.SuggestItem> items = new ArrayList<>();

        List<PositionPost> subjects = safeGet(() -> positionPostMapper.searchEnabledByKeyword(keyword, subjectLimit), List.of());
        for (PositionPost s : subjects) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("subjectId", s.getId());
            items.add(new HomeGuestVOs.SearchSuggestVO.SuggestItem(
                    "subject",
                    s.getName(),
                    "科目",
                    payload
            ));
        }

        if (serviceLimit > 0) {
            List<TeacherJobPosting> services = safeGet(() -> teacherJobPostingMapper.searchPublishedByTitle(keyword, serviceLimit), List.of());
            for (TeacherJobPosting p : services) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("serviceId", p.getId());
                items.add(new HomeGuestVOs.SearchSuggestVO.SuggestItem(
                        "service",
                        p.getTitle(),
                        "老师服务",
                        payload
                ));
            }
        }

        if (demandLimit > 0) {
            List<StudentJobPosting> demands = safeGet(() -> studentJobPostingMapper.searchPublishedByTitle(keyword, demandLimit), List.of());
            for (StudentJobPosting p : demands) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("demandId", p.getId());
                items.add(new HomeGuestVOs.SearchSuggestVO.SuggestItem(
                        "demand",
                        p.getTitle(),
                        "家长需求",
                        payload
                ));
            }
        }

        return new HomeGuestVOs.SearchSuggestVO(q, items);
    }

    @Override
    public List<SubjectTreeNodeVO> getSubjectTree() {
        return safeGet(subjectQueryService::getEnabledTree, List.of());
    }

    @Override
    public HomeGuestVOs.BannersVO getBanners(String city, String scene) {
        String effectiveCity = defaultIfBlank(city, homeGuestProperties.getDefaultCity());
        List<HomeGuestVOs.BannersVO.BannerItem> carousel = homeGuestProperties.getBanners()
                .getCarousel()
                .stream()
                .filter(b -> matchCity(b.getCities(), effectiveCity))
                .map(this::toBannerItem)
                .toList();
        List<HomeGuestVOs.BannersVO.BannerItem> cards = homeGuestProperties.getBanners()
                .getCards()
                .stream()
                .filter(b -> matchCity(b.getCities(), effectiveCity))
                .map(this::toBannerItem)
                .toList();
        return new HomeGuestVOs.BannersVO(carousel, cards);
    }

    @Override
    public HomeGuestVOs.HotTabsVO getHotTabs(String type, String city, Integer limit) {
        String effectiveType = defaultIfBlank(type, "service");
        String effectiveCity = defaultIfBlank(city, homeGuestProperties.getDefaultCity());
        int safeLimit = safeLimit(limit, 12, 50);

        List<HomeGuestProperties.TabItem> configured;
        if ("demand".equalsIgnoreCase(effectiveType)) {
            configured = homeGuestProperties.getHotTabs().getDemand();
        } else {
            configured = homeGuestProperties.getHotTabs().getService();
        }

        List<HomeGuestVOs.HotTabsVO.TabItem> tabs;
        if (CollectionUtil.isNotEmpty(configured)) {
            tabs = configured.stream()
                    .filter(t -> matchCity(t.getCities(), effectiveCity))
                    .limit(safeLimit)
                    .map(this::toTabItem)
                    .toList();
        } else {
            tabs = safeGet(
                    () -> buildDefaultSubjectTabs(safeLimit),
                    List.of(new HomeGuestVOs.HotTabsVO.TabItem("recommend", "推荐", Map.of()))
            );
        }

        return new HomeGuestVOs.HotTabsVO(effectiveType, tabs);
    }

    @Override
    public CursorPageResponse<HomeGuestVOs.HotServiceCardVO> getHotServices(String tabId,
                                                                            Long subjectId,
                                                                            String city,
                                                                            String mode,
                                                                            String sort,
                                                                            CursorPageRequest pageRequest) {
        CursorPageRequest req = ensurePageRequest(pageRequest);
        String effectiveSort = normalizeServiceSort(sort);

        List<TeacherJobPosting> postings = safeGet(
                () -> teacherJobPostingMapper.listPublishedSorted(
                        subjectId,
                        city,
                        mode,
                        effectiveSort,
                        req.getCursor(),
                        req.getPageSize()
                ),
                List.of()
        );

        if (CollectionUtil.isEmpty(postings)) {
            return new CursorPageResponse<>(null, true, List.of());
        }

        Map<Long, PositionPost> subjectMap = loadSubjectsByIds(
                postings.stream().map(TeacherJobPosting::getSubjectId).filter(Objects::nonNull).collect(Collectors.toSet())
        );

        Set<Long> tutorIds = postings.stream().map(TeacherJobPosting::getTutorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = loadUsersByIds(tutorIds);
        Map<Long, TeacherProfile> profileMap = loadTeacherProfilesByUserIds(tutorIds);

        List<HomeGuestVOs.HotServiceCardVO> list = new ArrayList<>();
        for (TeacherJobPosting p : postings) {
            PositionPost subject = subjectMap.get(p.getSubjectId());
            User user = userMap.get(p.getTutorId());
            TeacherProfile profile = profileMap.get(p.getTutorId());

            HomeGuestVOs.HotServiceCardVO.Subject subjectVo = new HomeGuestVOs.HotServiceCardVO.Subject(
                    p.getSubjectId(),
                    subject == null ? null : subject.getName()
            );

            HomeGuestVOs.HotServiceCardVO.Tutor tutorVo = new HomeGuestVOs.HotServiceCardVO.Tutor(
                    p.getTutorId(),
                    buildTutorDisplayName(profile, user),
                    user == null ? null : user.getAvatar(),
                    profile == null ? null : profile.getEducation(),
                    profile == null ? null : profile.getExperienceYears(),
                    profile == null ? null : profile.getRatePerHour()
            );

            List<String> tags = buildServiceTags(p, profile);

            list.add(new HomeGuestVOs.HotServiceCardVO(
                    p.getId(),
                    p.getTitle(),
                    subjectVo,
                    p.getPricePerHour(),
                    p.getMode(),
                    p.getCity(),
                    tutorVo,
                    tags
            ));
        }

        Long nextCursor = postings.get(postings.size() - 1).getId();
        boolean isLast = postings.size() < req.getPageSize();
        return new CursorPageResponse<>(nextCursor, isLast, list);
    }

    @Override
    public CursorPageResponse<HomeGuestVOs.HotDemandCardVO> getHotDemands(String tabId,
                                                                          Long subjectId,
                                                                          String city,
                                                                          String classMode,
                                                                          String sort,
                                                                          CursorPageRequest pageRequest) {
        CursorPageRequest req = ensurePageRequest(pageRequest);
        String effectiveSort = normalizeDemandSort(sort);

        List<StudentJobPosting> postings = safeGet(
                () -> studentJobPostingMapper.listPublishedSorted(
                        subjectId,
                        city,
                        classMode,
                        effectiveSort,
                        req.getCursor(),
                        req.getPageSize()
                ),
                List.of()
        );

        if (CollectionUtil.isEmpty(postings)) {
            return new CursorPageResponse<>(null, true, List.of());
        }

        Map<Long, PositionPost> subjectMap = loadSubjectsByIds(
                postings.stream().map(StudentJobPosting::getSubjectId).filter(Objects::nonNull).collect(Collectors.toSet())
        );

        Set<Long> parentIds = postings.stream().map(StudentJobPosting::getParentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = loadUsersByIds(parentIds);

        List<HomeGuestVOs.HotDemandCardVO> list = new ArrayList<>();
        for (StudentJobPosting p : postings) {
            PositionPost subject = subjectMap.get(p.getSubjectId());
            User parent = userMap.get(p.getParentId());

            HomeGuestVOs.HotDemandCardVO.Subject subjectVo = new HomeGuestVOs.HotDemandCardVO.Subject(
                    p.getSubjectId(),
                    subject == null ? null : subject.getName()
            );

            HomeGuestVOs.HotDemandCardVO.Budget budgetVo = new HomeGuestVOs.HotDemandCardVO.Budget(
                    p.getBudgetMin(),
                    p.getBudgetMax(),
                    "hour"
            );

            HomeGuestVOs.HotDemandCardVO.Parent parentVo = new HomeGuestVOs.HotDemandCardVO.Parent(
                    p.getParentId(),
                    buildParentDisplayName(parent),
                    parent == null ? null : parent.getAvatar()
            );

            List<String> tags = buildDemandTags(p);

            list.add(new HomeGuestVOs.HotDemandCardVO(
                    p.getId(),
                    p.getTitle(),
                    subjectVo,
                    budgetVo,
                    p.getClassMode(),
                    p.getCity(),
                    simplifyAddress(p.getAddress()),
                    p.getChildAge(),
                    scheduleText(p.getSchedule()),
                    parentVo,
                    tags
            ));
        }

        Long nextCursor = postings.get(postings.size() - 1).getId();
        boolean isLast = postings.size() < req.getPageSize();
        return new CursorPageResponse<>(nextCursor, isLast, list);
    }

    @Override
    public CursorPageResponse<HomeGuestVOs.HotTutorCardVO> getHotTutors(Long subjectId,
                                                                        String city,
                                                                        String mode,
                                                                        String sort,
                                                                        CursorPageRequest pageRequest) {
        CursorPageRequest req = ensurePageRequest(pageRequest);
        String effectiveSort = defaultIfBlank(sort, "recommend");

        List<HomeHotTutorAggRow> rows = safeGet(
                () -> teacherJobPostingMapper.listHotTutors(subjectId, city, mode, req.getCursor(), req.getPageSize()),
                List.of()
        );
        if (CollectionUtil.isEmpty(rows)) {
            return new CursorPageResponse<>(null, true, List.of());
        }

        Set<Long> tutorIds = rows.stream().map(HomeHotTutorAggRow::getTutorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = loadUsersByIds(tutorIds);
        Map<Long, TeacherProfile> profileMap = loadTeacherProfilesByUserIds(tutorIds);

        List<TeacherJobPosting> topServices = safeGet(
                () -> teacherJobPostingMapper.listTopNByTutorIds(new ArrayList<>(tutorIds), 2),
                List.of()
        );
        Map<Long, List<TeacherJobPosting>> tutorToServices = topServices.stream()
                .collect(Collectors.groupingBy(TeacherJobPosting::getTutorId));

        List<HomeGuestVOs.HotTutorCardVO> list = new ArrayList<>();
        for (HomeHotTutorAggRow row : rows) {
            Long tutorId = row.getTutorId();
            User user = userMap.get(tutorId);
            TeacherProfile profile = profileMap.get(tutorId);

            List<HomeGuestVOs.HotTutorCardVO.RepresentativeService> reps = tutorToServices
                    .getOrDefault(tutorId, List.of())
                    .stream()
                    .limit(2)
                    .map(s -> new HomeGuestVOs.HotTutorCardVO.RepresentativeService(s.getId(), s.getTitle(), s.getPricePerHour()))
                    .toList();

            List<String> subjectTags = buildTutorSubjectTags(profile);
            List<String> highlights = List.of("实名认证", "响应快");

            list.add(new HomeGuestVOs.HotTutorCardVO(
                    tutorId,
                    buildTutorDisplayName(profile, user),
                    user == null ? null : user.getAvatar(),
                    city,
                    profile == null ? null : profile.getEducation(),
                    profile == null ? null : profile.getExperienceYears(),
                    profile == null ? null : profile.getRatePerHour(),
                    subjectTags,
                    highlights,
                    reps
            ));
        }

        Long nextCursor = rows.get(rows.size() - 1).getCursorKey();
        boolean isLast = rows.size() < req.getPageSize();
        return new CursorPageResponse<>(nextCursor, isLast, list);
    }

    @Override
    public HomeGuestVOs.FooterLinksVO getFooterLinks() {
        List<HomeGuestVOs.FooterLinksVO.FooterLink> links = homeGuestProperties.getFooterLinks()
                .stream()
                .map(l -> new HomeGuestVOs.FooterLinksVO.FooterLink(l.getName(), l.getUrl()))
                .toList();
        return new HomeGuestVOs.FooterLinksVO(links);
    }

    private HomeGuestVOs.BannersVO.BannerItem toBannerItem(HomeGuestProperties.BannerItem b) {
        HomeGuestVOs.BannersVO.Link link = null;
        if (b.getLink() != null) {
            link = new HomeGuestVOs.BannersVO.Link(b.getLink().getType(), b.getLink().getUrl());
        }
        return new HomeGuestVOs.BannersVO.BannerItem(b.getId(), b.getTitle(), b.getSubtitle(), b.getImageUrl(), link);
    }

    private HomeGuestVOs.HotTabsVO.TabItem toTabItem(HomeGuestProperties.TabItem tab) {
        Map<String, Object> params = new HashMap<>();
        if (tab.getSubjectId() != null) {
            params.put("subjectId", tab.getSubjectId());
        }
        return new HomeGuestVOs.HotTabsVO.TabItem(
                defaultIfBlank(tab.getTabId(), tab.getSubjectId() == null ? "recommend" : "sub_" + tab.getSubjectId()),
                tab.getName(),
                params
        );
    }

    private List<HomeGuestVOs.HotTabsVO.TabItem> buildDefaultSubjectTabs(int limit) {
        List<SubjectTreeNodeVO> tree = subjectQueryService.getEnabledTree();
        List<SubjectTreeNodeVO> leaves = new ArrayList<>();
        for (SubjectTreeNodeVO root : tree) {
            collectLeaves(root, leaves);
        }
        List<HomeGuestVOs.HotTabsVO.TabItem> tabs = new ArrayList<>();
        tabs.add(new HomeGuestVOs.HotTabsVO.TabItem("recommend", "推荐", Map.of()));
        for (SubjectTreeNodeVO leaf : leaves.stream().limit(Math.max(0, limit - 1)).toList()) {
            tabs.add(new HomeGuestVOs.HotTabsVO.TabItem("sub_" + leaf.getId(), leaf.getName(), Map.of("subjectId", leaf.getId())));
        }
        return tabs;
    }

    private void collectLeaves(SubjectTreeNodeVO node, List<SubjectTreeNodeVO> out) {
        if (node == null) {
            return;
        }
        if (CollectionUtil.isEmpty(node.getChildren())) {
            if (node.getId() != null && node.getParentId() != null && node.getParentId() != 0L) {
                out.add(node);
            }
            return;
        }
        for (SubjectTreeNodeVO c : node.getChildren()) {
            collectLeaves(c, out);
        }
    }

    private CursorPageRequest ensurePageRequest(CursorPageRequest request) {
        CursorPageRequest req = request == null ? new CursorPageRequest() : request;
        if (req.getPageSize() == null) {
            req.setPageSize(10);
        }
        req.setPageSize(Math.min(Math.max(req.getPageSize(), 1), 100));
        return req;
    }

    private int safeLimit(Integer raw, int defaultValue, int max) {
        if (raw == null) {
            return defaultValue;
        }
        return Math.min(Math.max(raw, 1), max);
    }

    private boolean matchCity(List<String> cities, String city) {
        if (CollectionUtil.isEmpty(cities)) {
            return true;
        }
        if (city == null) {
            return false;
        }
        return cities.stream().anyMatch(c -> city.equalsIgnoreCase(c));
    }

    private String normalizeServiceSort(String sort) {
        String s = defaultIfBlank(sort, "recommend");
        if ("recommend".equals(s) || "latest".equals(s) || "priceAsc".equals(s) || "priceDesc".equals(s)) {
            return s;
        }
        return "recommend";
    }

    private String normalizeDemandSort(String sort) {
        String s = defaultIfBlank(sort, "recommend");
        if ("recommend".equals(s) || "latest".equals(s) || "budgetDesc".equals(s)) {
            return s;
        }
        return "recommend";
    }

    private Map<Long, PositionPost> loadSubjectsByIds(Set<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return Map.of();
        }
        List<PositionPost> list = safeGet(() -> positionPostMapper.selectByIds(new ArrayList<>(ids)), List.of());
        if (CollectionUtil.isEmpty(list)) {
            return Map.of();
        }
        return list.stream().collect(Collectors.toMap(PositionPost::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, User> loadUsersByIds(Set<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return Map.of();
        }
        List<User> list = safeGet(() -> userMapper.selectByIds(new ArrayList<>(ids)), List.of());
        if (CollectionUtil.isEmpty(list)) {
            return Map.of();
        }
        return list.stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, TeacherProfile> loadTeacherProfilesByUserIds(Set<Long> userIds) {
        if (CollectionUtil.isEmpty(userIds)) {
            return Map.of();
        }
        List<TeacherProfile> list = safeGet(() -> teacherProfileMapper.listByUserIds(new ArrayList<>(userIds)), List.of());
        if (CollectionUtil.isEmpty(list)) {
            return Map.of();
        }
        return list.stream().collect(Collectors.toMap(TeacherProfile::getUserId, Function.identity(), (a, b) -> a));
    }

    private String buildTutorDisplayName(TeacherProfile profile, User user) {
        if (profile != null && StrUtil.isNotBlank(profile.getRealName())) {
            return maskRealNameAsTeacher(profile.getRealName());
        }
        if (user != null && StrUtil.isNotBlank(user.getName())) {
            return maskNickname(user.getName());
        }
        if (user != null && StrUtil.isNotBlank(user.getPhone())) {
            return "教师" + last4(user.getPhone());
        }
        return "老师";
    }

    private String buildParentDisplayName(User user) {
        if (user != null && StrUtil.isNotBlank(user.getPhone())) {
            return "家长" + last4(user.getPhone());
        }
        if (user != null && StrUtil.isNotBlank(user.getName())) {
            return "家长" + maskNickname(user.getName());
        }
        return "家长";
    }

    private String maskRealNameAsTeacher(String realName) {
        String n = realName.trim();
        if (n.isBlank()) {
            return "老师";
        }
        return n.substring(0, 1) + "老师";
    }

    private String maskNickname(String name) {
        String n = name.trim();
        if (n.isBlank()) {
            return "用户";
        }
        if (n.length() <= 1) {
            return n + "**";
        }
        return n.substring(0, 1) + "**";
    }

    private String last4(String s) {
        if (s == null) {
            return "";
        }
        String v = s.trim();
        if (v.length() <= 4) {
            return v;
        }
        return v.substring(v.length() - 4);
    }

    private List<String> buildServiceTags(TeacherJobPosting p, TeacherProfile profile) {
        List<String> tags = new ArrayList<>();
        if (StrUtil.isNotBlank(p.getMode())) {
            if ("online".equalsIgnoreCase(p.getMode())) {
                tags.add("可线上");
            } else if ("offline".equalsIgnoreCase(p.getMode())) {
                tags.add("可线下");
            } else if ("both".equalsIgnoreCase(p.getMode())) {
                tags.add("可线上");
                tags.add("可线下");
            }
        }
        if (profile != null && profile.getExperienceYears() != null) {
            tags.add("经验" + profile.getExperienceYears() + "年");
        }
        if (p.getPricePerHour() != null) {
            tags.add("¥" + p.getPricePerHour() + "/小时");
        }
        return tags;
    }

    private List<String> buildDemandTags(StudentJobPosting p) {
        List<String> tags = new ArrayList<>();
        if (p.getBudgetMin() != null || p.getBudgetMax() != null) {
            String min = p.getBudgetMin() == null ? "" : p.getBudgetMin().stripTrailingZeros().toPlainString();
            String max = p.getBudgetMax() == null ? "" : p.getBudgetMax().stripTrailingZeros().toPlainString();
            if (!min.isBlank() && !max.isBlank()) {
                tags.add("预算" + min + "-" + max);
            } else if (!max.isBlank()) {
                tags.add("预算≤" + max);
            } else if (!min.isBlank()) {
                tags.add("预算≥" + min);
            }
        }
        String schedule = scheduleText(p.getSchedule());
        if (StrUtil.isNotBlank(schedule)) {
            tags.add(schedule);
        }
        if (StrUtil.isNotBlank(p.getClassMode())) {
            if ("online".equalsIgnoreCase(p.getClassMode())) {
                tags.add("线上");
            } else if ("offline".equalsIgnoreCase(p.getClassMode())) {
                tags.add("线下");
            } else if ("both".equalsIgnoreCase(p.getClassMode())) {
                tags.add("线上/线下");
            }
        }
        return tags;
    }

    private List<String> buildTutorSubjectTags(TeacherProfile profile) {
        if (profile == null || StrUtil.isBlank(profile.getSubject())) {
            return List.of();
        }
        String raw = profile.getSubject().trim();
        if (raw.isBlank()) {
            return List.of();
        }
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

    private String scheduleText(String schedule) {
        if (schedule == null) {
            return null;
        }
        String s = schedule.trim();
        if (s.isBlank()) {
            return null;
        }
        if (s.length() > 20) {
            return s.substring(0, 20) + "...";
        }
        return s;
    }

    private String simplifyAddress(String address) {
        if (address == null) {
            return null;
        }
        String a = address.trim();
        if (a.isBlank()) {
            return null;
        }
        // 未登录展示脱敏：只展示到区/县/市一级，避免泄露具体住址
        int idx = a.indexOf("区");
        if (idx > 0) {
            return a.substring(0, idx + 1);
        }
        idx = a.indexOf("县");
        if (idx > 0) {
            return a.substring(0, idx + 1);
        }
        idx = a.indexOf("市");
        if (idx > 0) {
            return a.substring(0, idx + 1);
        }
        if (a.length() > 6) {
            return a.substring(0, 6) + "...";
        }
        return a;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("Proxy-Client-IP");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String v = value.trim();
        return v.isBlank() ? defaultValue : v;
    }

    private <T> T safeGet(Supplier<T> supplier, T fallback) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return fallback;
        }
    }
}
