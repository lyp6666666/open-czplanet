package com.ai.tutor.appointment.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = TestBackdoorSeedMapperTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:test_backdoor_seed_testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class TestBackdoorSeedMapperTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestBackdoorSeedMapper testBackdoorSeedMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM student_job_posting");
    }

    @Test
    void upsertExclusiveStudentJobPostingShouldRefreshStaleTeachingModeAndVisibilityFields() {
        jdbcTemplate.update("""
                INSERT INTO student_job_posting (
                    id, parent_id, subject_name, subject_is_other, title, description, student_gender,
                    teacher_gender_preference, teacher_requirement_detail, class_mode, frequency_per_week,
                    publisher_identity, budget_min, budget_max, stage_code, education_requirement, schedule,
                    biz_status, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                666601L, 12345L, "旧科目", 1, "旧标题", "旧描述", "female",
                "female", "旧要求", null, 1,
                "PARENT", 99, 199, "JUNIOR", "BACHELOR", "[\"Sun 08:00-09:00\"]",
                0, 0
        );

        int affected = testBackdoorSeedMapper.upsertExclusiveStudentJobPosting(666601L, 26666666666L);

        assertThat(affected).isPositive();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT class_mode FROM student_job_posting WHERE id = ?",
                String.class,
                666601L
        )).isEqualTo("online");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT publisher_identity FROM student_job_posting WHERE id = ?",
                String.class,
                666601L
        )).isEqualTo("STUDENT_SELF");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT biz_status FROM student_job_posting WHERE id = ?",
                Integer.class,
                666601L
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM student_job_posting WHERE id = ?",
                Integer.class,
                666601L
        )).isEqualTo(1);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan("com.ai.tutor.appointment.mapper")
    static class TestConfig {
    }
}
