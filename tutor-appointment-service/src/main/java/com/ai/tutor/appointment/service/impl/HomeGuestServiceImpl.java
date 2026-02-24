package com.ai.tutor.appointment.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.ai.tutor.appointment.config.HomeGuestProperties;
import com.ai.tutor.appointment.storage.MinioProperties;
import com.ai.tutor.appointment.mapper.*;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.home.HomeHotTutorAggRow;
import com.ai.tutor.appointment.model.entity.*;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.SubjectTreeNodeVO;
import com.ai.tutor.appointment.model.vo.home.HomeGuestVOs;
import com.ai.tutor.appointment.service.HomeGuestService;
import com.ai.tutor.appointment.service.SubjectQueryService;
import com.ai.tutor.appointment.utils.CityCatalog;
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

    @Resource
    private MinioProperties minioProperties;

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
        String effectiveCity = CityCatalog.normalizeCityForFilter(city);
        boolean allowOnlineFallback = mode == null || mode.trim().isEmpty() || "both".equalsIgnoreCase(mode.trim());

        List<TeacherJobPosting> postings;
        if (req.getCursor() == null && effectiveCity != null) {
            postings = safeGet(
                    () -> teacherJobPostingMapper.listPublishedSorted(
                            subjectId,
                            effectiveCity,
                            mode,
                            effectiveSort,
                            null,
                            req.getPageSize()
                    ),
                    List.of()
            );
            if (allowOnlineFallback && postings.size() < req.getPageSize()) {
                int need = req.getPageSize() - postings.size();
                List<TeacherJobPosting> fallback = safeGet(
                        () -> teacherJobPostingMapper.listPublishedSorted(
                                subjectId,
                                null,
                                "online",
                                effectiveSort,
                                null,
                                Math.max(req.getPageSize() * 2, need)
                        ),
                        List.of()
                );
                Set<Long> seen = postings.stream().map(TeacherJobPosting::getId).filter(Objects::nonNull).collect(Collectors.toSet());
                List<TeacherJobPosting> merged = new ArrayList<>(postings);
                for (TeacherJobPosting it : fallback) {
                    if (it == null || it.getId() == null || seen.contains(it.getId())) {
                        continue;
                    }
                    merged.add(it);
                    seen.add(it.getId());
                    if (merged.size() >= req.getPageSize()) {
                        break;
                    }
                }
                postings = merged;
            }
        } else {
            postings = safeGet(
                    () -> teacherJobPostingMapper.listPublishedSorted(
                            subjectId,
                            effectiveCity,
                            mode,
                            effectiveSort,
                            req.getCursor(),
                            req.getPageSize()
                    ),
                    List.of()
            );
        }

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
        String effectiveCity = CityCatalog.normalizeCityForFilter(city);
        boolean allowOnlineFallback = classMode == null || classMode.trim().isEmpty() || "both".equalsIgnoreCase(classMode.trim());

        List<StudentJobPosting> postings;
        if (req.getCursor() == null && effectiveCity != null) {
            postings = safeGet(
                    () -> studentJobPostingMapper.listPublishedSorted(
                            subjectId,
                            effectiveCity,
                            classMode,
                            null,
                            effectiveSort,
                            null,
                            req.getPageSize()
                    ),
                    List.of()
            );
            if (allowOnlineFallback && postings.size() < req.getPageSize()) {
                int need = req.getPageSize() - postings.size();
                List<StudentJobPosting> fallback = safeGet(
                        () -> studentJobPostingMapper.listPublishedSorted(
                                subjectId,
                                null,
                                "online",
                                null,
                                effectiveSort,
                                null,
                                Math.max(req.getPageSize() * 2, need)
                        ),
                        List.of()
                );
                Set<Long> seen = postings.stream().map(StudentJobPosting::getId).filter(Objects::nonNull).collect(Collectors.toSet());
                List<StudentJobPosting> merged = new ArrayList<>(postings);
                for (StudentJobPosting it : fallback) {
                    if (it == null || it.getId() == null || seen.contains(it.getId())) {
                        continue;
                    }
                    merged.add(it);
                    seen.add(it.getId());
                    if (merged.size() >= req.getPageSize()) {
                        break;
                    }
                }
                postings = merged;
            }
        } else {
            postings = safeGet(
                    () -> studentJobPostingMapper.listPublishedSorted(
                            subjectId,
                            effectiveCity,
                            classMode,
                            null,
                            effectiveSort,
                            req.getCursor(),
                            req.getPageSize()
                    ),
                    List.of()
            );
        }

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
        String effectiveCity = CityCatalog.normalizeCityForFilter(city);
        boolean allowOnlineFallback = mode == null || mode.trim().isEmpty() || "both".equalsIgnoreCase(mode.trim());

        List<HomeHotTutorAggRow> rows;
        if (req.getCursor() == null && effectiveCity != null) {
            rows = safeGet(
                    () -> teacherJobPostingMapper.listHotTutors(subjectId, effectiveCity, mode, null, req.getPageSize()),
                    List.of()
            );
            if (allowOnlineFallback && rows.size() < req.getPageSize()) {
                int need = req.getPageSize() - rows.size();
                List<HomeHotTutorAggRow> fallback = safeGet(
                        () -> teacherJobPostingMapper.listHotTutors(subjectId, null, "online", null, Math.max(req.getPageSize() * 2, need)),
                        List.of()
                );
                Set<Long> seen = rows.stream().map(HomeHotTutorAggRow::getTutorId).filter(Objects::nonNull).collect(Collectors.toSet());
                List<HomeHotTutorAggRow> merged = new ArrayList<>(rows);
                for (HomeHotTutorAggRow it : fallback) {
                    if (it == null || it.getTutorId() == null || seen.contains(it.getTutorId())) {
                        continue;
                    }
                    merged.add(it);
                    seen.add(it.getTutorId());
                    if (merged.size() >= req.getPageSize()) {
                        break;
                    }
                }
                rows = merged;
            }
        } else {
            rows = safeGet(
                    () -> teacherJobPostingMapper.listHotTutors(subjectId, effectiveCity, mode, req.getCursor(), req.getPageSize()),
                    List.of()
            );
        }
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
            List<String> highlights = new ArrayList<>();
            if (profile != null
                    && profile.getRealnameVerifyStatus() != null
                    && profile.getEduVerifyStatus() != null
                    && profile.getRealnameVerifyStatus() == 2
                    && profile.getEduVerifyStatus() == 2) {
                highlights.add("实名认证");
                highlights.add("学籍认证");
            }
            highlights.add("响应快");

            list.add(new HomeGuestVOs.HotTutorCardVO(
                    tutorId,
                    buildTutorDisplayName(profile, user),
                    user == null ? null : user.getAvatar(),
                    effectiveCity == null ? CityCatalog.national() : effectiveCity,
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
        String imageUrl = normalizeBannerImageUrl(b.getImageUrl());
        return new HomeGuestVOs.BannersVO.BannerItem(b.getId(), b.getTitle(), b.getSubtitle(), imageUrl, link);
    }

    /**
     * Banner 图片地址兼容策略：
     * - bannersUseMinio=false：原样透传（兼容旧的 /banners/* 静态资源）；
     * - bannersUseMinio=true：imageUrl 视为 objectKey，拼 publicBaseUrl 输出完整 URL。
     */
    private String normalizeBannerImageUrl(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return null;
        }
        if (Boolean.FALSE.equals(homeGuestProperties.getBannersUseMinio())) {
            return v;
        }
        if (v.startsWith("http://") || v.startsWith("https://")) {
            return v;
        }
        String base = minioProperties == null ? null : minioProperties.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            return v;
        }
        String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String p = v.startsWith("/") ? v.substring(1) : v;
        return b + "/" + p;
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
        if (CityCatalog.national().equals(city)) {
            return true;
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
