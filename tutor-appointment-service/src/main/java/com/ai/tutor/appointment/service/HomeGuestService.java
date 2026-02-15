package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.home.HomeGuestVOs;
import com.ai.tutor.appointment.model.vo.SubjectTreeNodeVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 未登录首页（Guest Home）服务。
 *
 * 约束：
 * - 所有接口均无需登录；
 * - 返回内容用于首页展示，必须避免泄露敏感信息（手机号、详细地址、身份证明等）。
 */
public interface HomeGuestService {

    HomeGuestVOs.HomeConfigVO getHomeConfig(String city);

    HomeGuestVOs.GeoLocateVO locate(HttpServletRequest request);

    HomeGuestVOs.HotWordsVO getHotWords(String city, Integer limit);

    HomeGuestVOs.SearchSuggestVO suggest(String q, String city, Integer limit);

    List<SubjectTreeNodeVO> getSubjectTree();

    HomeGuestVOs.BannersVO getBanners(String city, String scene);

    HomeGuestVOs.HotTabsVO getHotTabs(String type, String city, Integer limit);

    CursorPageResponse<HomeGuestVOs.HotServiceCardVO> getHotServices(String tabId,
                                                                     Long subjectId,
                                                                     String city,
                                                                     String mode,
                                                                     String sort,
                                                                     CursorPageRequest pageRequest);

    CursorPageResponse<HomeGuestVOs.HotDemandCardVO> getHotDemands(String tabId,
                                                                   Long subjectId,
                                                                   String city,
                                                                   String classMode,
                                                                   String sort,
                                                                   CursorPageRequest pageRequest);

    CursorPageResponse<HomeGuestVOs.HotTutorCardVO> getHotTutors(Long subjectId,
                                                                 String city,
                                                                 String mode,
                                                                 String sort,
                                                                 CursorPageRequest pageRequest);

    HomeGuestVOs.FooterLinksVO getFooterLinks();
}

