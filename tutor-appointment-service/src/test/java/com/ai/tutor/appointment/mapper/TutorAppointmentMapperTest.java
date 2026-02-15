package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.TutorAppointment;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = TutorAppointmentMapperTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:tutor_appointment_testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class TutorAppointmentMapperTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TutorAppointmentMapper tutorAppointmentMapper;

    @Test
    void confirmRescheduleShouldMoveProposedTimeToStartTime() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        TutorAppointment appointment = TutorAppointment.builder()
                .parentId(1L)
                .tutorId(2L)
                .subjectId(101L)
                .startTime(start)
                .durationMinutes(60)
                .status(2)
                .createdBy(1L)
                .build();
        int inserted = tutorAppointmentMapper.insert(appointment);
        assertThat(inserted).isEqualTo(1);
        assertThat(appointment.getId()).isNotNull();

        LocalDateTime proposed = start.plusDays(2).withNano(0);
        TutorAppointment reschedule = TutorAppointment.builder()
                .id(appointment.getId())
                .status(3)
                .proposedStartTime(proposed)
                .proposedBy(2L)
                .build();
        int updated = tutorAppointmentMapper.updateById(reschedule);
        assertThat(updated).isEqualTo(1);

        int confirmed = tutorAppointmentMapper.confirmReschedule(appointment.getId());
        assertThat(confirmed).isEqualTo(1);

        TutorAppointment db = tutorAppointmentMapper.selectById(appointment.getId());
        assertThat(db.getStatus()).isEqualTo(2);
        assertThat(db.getStartTime()).isEqualTo(proposed);
        assertThat(db.getProposedStartTime()).isNull();
        assertThat(db.getProposedBy()).isNull();
    }

    @Test
    void acceptIfPendingShouldBeIdempotent() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        TutorAppointment appointment = TutorAppointment.builder()
                .parentId(1L)
                .tutorId(2L)
                .subjectId(101L)
                .startTime(start)
                .durationMinutes(60)
                .status(1)
                .createdBy(1L)
                .build();
        int inserted = tutorAppointmentMapper.insert(appointment);
        assertThat(inserted).isEqualTo(1);
        assertThat(appointment.getId()).isNotNull();

        int first = tutorAppointmentMapper.acceptIfPending(appointment.getId());
        int second = tutorAppointmentMapper.acceptIfPending(appointment.getId());
        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(0);

        TutorAppointment db = tutorAppointmentMapper.selectById(appointment.getId());
        assertThat(db.getStatus()).isEqualTo(2);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan("com.ai.tutor.appointment.mapper")
    static class TestConfig {
    }
}
