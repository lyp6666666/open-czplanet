package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.config.HomeGuestProperties;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.SubjectTreeNodeVO;
import com.ai.tutor.appointment.model.vo.home.HomeGuestVOs;
import com.ai.tutor.appointment.service.impl.HomeGuestServiceImpl;
import com.ai.tutor.appointment.service.impl.SubjectQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = HomeGuestServiceImplTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:home_guest_testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class HomeGuestServiceImplTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HomeGuestService homeGuestService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM teacher_job_posting");
        jdbcTemplate.execute("DELETE FROM student_job_posting");
        jdbcTemplate.execute("DELETE FROM teacher_profile");
        jdbcTemplate.execute("DELETE FROM user");
        jdbcTemplate.execute("DELETE FROM position_post");

        jdbcTemplate.update("INSERT INTO position_post(id, parent_id, name, grade, description, sort, enable_status) VALUES (1, 0, '初中', '初中', '', 1, 1)");
        jdbcTemplate.update("INSERT INTO position_post(id, parent_id, name, grade, description, sort, enable_status) VALUES (2, 1, '数学', '初中', '', 1, 1)");

        jdbcTemplate.update("INSERT INTO user(id, name, phone, avatar, user_type, status) VALUES (1001, 'Tom', '13800001111', 'https://img/a.png', 1, 0)");
        jdbcTemplate.update("INSERT INTO user(id, name, phone, avatar, user_type, status) VALUES (1002, 'Jerry', '13800002222', 'https://img/b.png', 1, 0)");
        jdbcTemplate.update("INSERT INTO user(id, name, phone, avatar, user_type, status) VALUES (2001, 'ParentA', '13900003333', 'https://img/p.png', 2, 0)");

        jdbcTemplate.update("INSERT INTO teacher_profile(user_id, real_name, education, subject, experience_years, rate_per_hour, status) VALUES (1001, '张三', '本科', '数学', 3, 120.00, 1)");
        jdbcTemplate.update("INSERT INTO teacher_profile(user_id, real_name, education, subject, experience_years, rate_per_hour, status) VALUES (1002, '李四', '研究生', '数学', 5, 80.00, 1)");

        jdbcTemplate.update("INSERT INTO teacher_job_posting(id, tutor_id, subject_id, title, description, price_per_hour, mode, city, status) VALUES (10, 1001, 2, '初中数学一对一', '', 120.00, 'online', '北京', 1)");
        jdbcTemplate.update("INSERT INTO teacher_job_posting(id, tutor_id, subject_id, title, description, price_per_hour, mode, city, status) VALUES (11, 1001, 2, '初中数学冲刺', '', 150.00, 'online', '北京', 1)");
        jdbcTemplate.update("INSERT INTO teacher_job_posting(id, tutor_id, subject_id, title, description, price_per_hour, mode, city, status) VALUES (12, 1002, 2, '数学提分课', '', 80.00, 'online', '北京', 1)");

        jdbcTemplate.update("INSERT INTO student_job_posting(id, parent_id, subject_id, title, description, child_age, class_mode, city, address, budget_min, budget_max, schedule, status) VALUES (20, 2001, 2, '找初中数学老师', '', 13, 'offline', '北京', '北京市海淀区xxx路', 100.00, 160.00, '周末', 1)");
    }

    @Test
    void subjectTreeShouldReturnHierarchy() {
        List<SubjectTreeNodeVO> tree = homeGuestService.getSubjectTree();
        assertThat(tree).isNotEmpty();
        assertThat(tree.get(0).getChildren()).isNotEmpty();
        assertThat(tree.get(0).getChildren().get(0).getName()).isEqualTo("数学");
    }

    @Test
    void suggestShouldReturnSubjectAndPostings() {
        HomeGuestVOs.SearchSuggestVO vo = homeGuestService.suggest("数学", "北京", 10);
        assertThat(vo.getList()).isNotEmpty();
        assertThat(vo.getList().stream().anyMatch(i -> "subject".equals(i.getType()))).isTrue();
        assertThat(vo.getList().stream().anyMatch(i -> "service".equals(i.getType()))).isTrue();
        assertThat(vo.getList().stream().anyMatch(i -> "demand".equals(i.getType()))).isTrue();
    }

    @Test
    void hotServicesShouldReturnCardsAndCursor() {
        CursorPageRequest req = new CursorPageRequest();
        req.setCursor(null);
        req.setPageSize(2);
        CursorPageResponse<HomeGuestVOs.HotServiceCardVO> page = homeGuestService.getHotServices(
                "recommend",
                2L,
                "北京",
                "online",
                "priceAsc",
                req
        );
        assertThat(page.getList()).hasSize(2);
        assertThat(page.getList().get(0).getPricePerHour()).isEqualTo(new BigDecimal("80.00"));
        assertThat(page.getNextCursor()).isNotNull();
        assertThat(page.getIsLast()).isFalse();
    }

    @Test
    void hotTutorsShouldAggregateTutors() {
        CursorPageRequest req = new CursorPageRequest();
        req.setCursor(null);
        req.setPageSize(10);
        CursorPageResponse<HomeGuestVOs.HotTutorCardVO> page = homeGuestService.getHotTutors(
                2L,
                "北京",
                "online",
                "recommend",
                req
        );
        assertThat(page.getList()).hasSize(2);
        assertThat(page.getList().get(0).getRepresentativeServices()).isNotEmpty();
        assertThat(page.getNextCursor()).isNotNull();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan("com.ai.tutor.appointment.mapper")
    @Import({HomeGuestServiceImpl.class, SubjectQueryServiceImpl.class, HomeGuestServiceImplTest.TestBeans.class})
    static class TestConfig {
    }

    static class TestBeans {
        @Bean
        public HomeGuestProperties homeGuestProperties() {
            return new HomeGuestProperties();
        }
    }
}
