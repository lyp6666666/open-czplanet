package com.ai.tutor.appointment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 未登录首页（仿 BOSS 直聘信息架构）的运营配置。
 *
 * 设计原则：
 * 1) 首页接口必须可用：即便没有任何运营配置，也要返回“可渲染”的默认结构；
 * 2) 数据可演进：从配置驱动起步，后续可平滑切到 DB/运营后台而不改 API；
 * 3) 避免前端写死：导航、热搜、banner、footer 都通过接口下发。
 */
@Data
@Component
@ConfigurationProperties(prefix = "home.guest")
public class HomeGuestProperties {

    /**
     * 默认城市（当无法定位、或客户端未传 city 时使用）。
     */
    private String defaultCity = "北京";

    /**
     * 是否允许切换城市（决定前端是否展示城市选择器入口）。
     */
    private Boolean citySelectable = true;

    /**
     * 搜索框配置。
     */
    private Search search = new Search();

    /**
     * 顶部导航（未登录）。
     */
    private List<NavItem> nav = new ArrayList<>();

    /**
     * 登录/注册入口配置（未登录）。
     */
    private AuthEntry authEntry = new AuthEntry();

    /**
     * 热搜词（未登录首页展示）。
     */
    private List<HotWord> hotWords = new ArrayList<>();

    /**
     * Banner 轮播与卡片。
     */
    private Banner banners = new Banner();

    /**
     * Banner 图片地址是否按 MinIO 对外域名拼装：
     * - true：imageUrl 配置为 objectKey（例如 banners/carousel-1.svg），后端自动拼 publicBaseUrl；
     * - false：imageUrl 原样透传（兼容旧的 /banners/* 静态资源方案）。
     */
    private Boolean bannersUseMinio = false;

    /**
     * 热门区 tab（用于“热门服务/热门需求”）。
     * - 允许配置固定 tab；
     * - 若为空，则由后端根据科目树自动生成默认 tab。
     */
    private HotTabs hotTabs = new HotTabs();

    /**
     * 页脚链接。
     */
    private List<FooterLink> footerLinks = new ArrayList<>();

    @Data
    public static class Search {
        private String placeholder = "搜索科目/老师/需求，例如：初中数学";
        private String defaultMode = "both";
    }

    @Data
    public static class NavItem {
        private String key;
        private String name;
        private String link;
    }

    @Data
    public static class AuthEntry {
        private String loginText = "登录/注册";
        private String link = "/login";
    }

    @Data
    public static class HotWord {
        private String word;
        private String type = "keyword";
        private List<String> cities;
    }

    @Data
    public static class Banner {
        private List<BannerItem> carousel = new ArrayList<>();
        private List<BannerItem> cards = new ArrayList<>();
    }

    @Data
    public static class BannerItem {
        private String id;
        private String title;
        private String subtitle;
        private String imageUrl;
        private Link link;
        private List<String> cities;
    }

    @Data
    public static class Link {
        private String type = "ROUTE";
        private String url;
    }

    @Data
    public static class HotTabs {
        private Integer subjectTabLimit = 8;
        private List<TabItem> service = new ArrayList<>();
        private List<TabItem> demand = new ArrayList<>();
    }

    @Data
    public static class TabItem {
        private String tabId;
        private String name;
        private Long subjectId;
        private List<String> cities;
    }

    @Data
    public static class FooterLink {
        private String name;
        private String url;
    }
}
