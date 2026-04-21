package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.vo.CursorPageResponse;
import com.ai.tutor.appointment.model.vo.parent.ParentTutorVOs;
import com.ai.tutor.appointment.service.impl.ParentTutorBrowseServiceImpl;
import com.ai.tutor.appointment.storage.MinioProperties;
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

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ParentTutorBrowseServiceImplTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:parent_tutor_browse_testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class ParentTutorBrowseServiceImplTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ParentTutorBrowseService parentTutorBrowseService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM teacher_profile");
        jdbcTemplate.execute("DELETE FROM user");

        jdbcTemplate.update("INSERT INTO user(id, name, phone, avatar, user_type, status) VALUES (1001, 'TeacherA', '13800001111', 'https://img/a.png', 1, 0)");
        jdbcTemplate.update("INSERT INTO user(id, name, phone, avatar, user_type, status) VALUES (1002, 'TeacherB', '13800002222', 'https://img/b.png', 1, 0)");
        jdbcTemplate.update("INSERT INTO user(id, name, phone, avatar, user_type, status) VALUES (2001, 'StudentA', '13900003333', 'https://img/p.png', 2, 0)");

        jdbcTemplate.update("INSERT INTO teacher_profile(id, user_id, real_name, education, subject, experience_years, rate_per_hour, introduction, city, status) VALUES (11, 1001, '张老师', '本科', '数学', 3, 120.00, '擅长提分', '南昌', NULL)");
        jdbcTemplate.update("INSERT INTO teacher_profile(id, user_id, real_name, education, subject, experience_years, rate_per_hour, introduction, city, status) VALUES (12, 1002, '李老师', '硕士', '英语', 5, 150.00, '擅长口语', '南昌', 1)");
    }

    @Test
    void pageTutorsShouldIncludeLegacyRowsWithNullStatus() {
        CursorPageRequest req = new CursorPageRequest();
        req.setCursor(null);
        req.setPageSize(10);

        CursorPageResponse<ParentTutorVOs.TutorCardVO> page = parentTutorBrowseService.pageTutors(
                2001L,
                null,
                "南昌",
                null,
                null,
                null,
                req
        );

        assertThat(page.getList()).hasSize(2);
        assertThat(page.getList().stream().map(ParentTutorVOs.TutorCardVO::getDisplayName).collect(Collectors.toList()))
                .containsExactly("李老师", "张老师");
        assertThat(page.getIsLast()).isTrue();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan("com.ai.tutor.appointment.mapper")
    @Import({ParentTutorBrowseServiceImpl.class, ParentTutorBrowseServiceImplTest.TestBeans.class})
    static class TestConfig {
    }

    static class TestBeans {
        @Bean
        public MinioProperties minioProperties() {
            MinioProperties p = new MinioProperties();
            p.setEnabled(false);
            return p;
        }
    }
}
