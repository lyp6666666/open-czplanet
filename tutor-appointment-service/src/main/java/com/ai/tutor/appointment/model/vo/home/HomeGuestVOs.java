package com.ai.tutor.appointment.model.vo.home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 未登录首页（Guest Home）相关 VO 集合。
 *
 * 说明：
 * - 这里的 VO 面向“首页卡片展示”，字段比 DB 实体更贴近 UI；
 * - 不引入敏感字段（手机号、详细地址等），避免未登录场景信息泄露。
 */
public class HomeGuestVOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HomeConfigVO {
        private String defaultCity;
        private Boolean citySelectable;
        private Search search;
        private List<NavItem> nav;
        private AuthEntry authEntry;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Search {
            private String placeholder;
            private String defaultMode;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class NavItem {
            private String key;
            private String name;
            private String link;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AuthEntry {
            private String loginText;
            private String link;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoLocateVO {
        private String ip;
        private String city;
        private String province;
        private String cityCode;
        private List<String> suggestCities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotWordsVO {
        private LocalDateTime updatedAt;
        private List<HotWord> list;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class HotWord {
            private String word;
            private String type;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchSuggestVO {
        private String q;
        private List<SuggestItem> list;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SuggestItem {
            private String type;
            private String title;
            private String subtitle;
            private Map<String, Object> payload;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BannersVO {
        private List<BannerItem> carousel;
        private List<BannerItem> cards;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class BannerItem {
            private String id;
            private String title;
            private String subtitle;
            private String imageUrl;
            private Link link;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Link {
            private String type;
            private String url;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotTabsVO {
        private String type;
        private List<TabItem> tabs;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TabItem {
            private String tabId;
            private String name;
            private Map<String, Object> params;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotServiceCardVO {
        private Long serviceId;
        private String title;
        private Subject subject;
        private BigDecimal pricePerHour;
        private String mode;
        private String city;
        private Tutor tutor;
        private List<String> tags;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Subject {
            private Long id;
            private String name;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Tutor {
            private Long userId;
            private String displayName;
            private String avatar;
            private String education;
            private Integer experienceYears;
            private BigDecimal ratePerHour;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotDemandCardVO {
        private Long demandId;
        private String title;
        private Subject subject;
        private Budget budget;
        private String classMode;
        private String city;
        private String addressSimple;
        private Integer childAge;
        private String scheduleText;
        private Parent parent;
        private List<String> tags;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Subject {
            private Long id;
            private String name;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Budget {
            private BigDecimal min;
            private BigDecimal max;
            private String unit;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Parent {
            private Long userId;
            private String displayName;
            private String avatar;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotTutorCardVO {
        private Long userId;
        private String displayName;
        private String avatar;
        private String city;
        private String education;
        private Integer experienceYears;
        private BigDecimal ratePerHour;
        private List<String> subjectTags;
        private List<String> highlights;
        private List<RepresentativeService> representativeServices;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RepresentativeService {
            private Long serviceId;
            private String title;
            private BigDecimal pricePerHour;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FooterLinksVO {
        private List<FooterLink> links;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class FooterLink {
            private String name;
            private String url;
        }
    }
}

