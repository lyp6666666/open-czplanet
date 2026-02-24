package com.ai.tutor.appointment.service;

import com.ai.tutor.appointment.mapper.PositionPostMapper;
import com.ai.tutor.appointment.mapper.StudentJobPostingMapper;
import com.ai.tutor.appointment.mapper.UserMapper;
import com.ai.tutor.appointment.model.dto.common.CursorPageRequest;
import com.ai.tutor.appointment.model.dto.job.CreateStudentJobPostingRequest;
import com.ai.tutor.appointment.model.entity.PositionPost;
import com.ai.tutor.appointment.model.entity.StudentJobPosting;
import com.ai.tutor.appointment.model.entity.User;
import com.ai.tutor.appointment.model.vo.DemandViewVO;
import com.ai.tutor.appointment.service.impl.StudentJobPostingServiceImpl;
import com.ai.tutor.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StudentJobPostingServiceImplTest {

    private PositionPostMapper positionPostMapper;
    private StudentJobPostingMapper studentJobPostingMapper;
    private UserMapper userMapper;
    private StudentJobPostingServiceImpl service;

    @BeforeEach
    void setUp() {
        positionPostMapper = mock(PositionPostMapper.class);
        studentJobPostingMapper = mock(StudentJobPostingMapper.class);
        userMapper = mock(UserMapper.class);
        service = new StudentJobPostingServiceImpl();
        ReflectionTestUtils.setField(service, "studentJobPostingMapper", studentJobPostingMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "positionPostMapper", positionPostMapper);
    }

    @Test
    void createShouldRejectOfflineWithoutAddress() {
        CreateStudentJobPostingRequest req = new CreateStudentJobPostingRequest();
        req.setSubjectId(201L);
        req.setSubjectName("数学");
        req.setTitle("初中数学一对一");
        req.setDescription("描述");
        req.setStudentGender("male");
        req.setGradeCode("JUNIOR1");
        req.setClassMode("offline");
        req.setCity("北京");
        req.setAddress(null);
        req.setFrequencyPerWeek(2);
        req.setStageCode("JUNIOR");
        req.setEducationRequirement("BACHELOR");
        req.setPublisherIdentity("PARENT");

        PositionPost post = new PositionPost();
        post.setId(201L);
        post.setName("数学");
        when(positionPostMapper.selectByIds(anyList())).thenReturn(java.util.List.of(post));

        assertThatThrownBy(() -> service.create(req, 101L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("授课地址");
    }

    @Test
    void createShouldNormalizeEducationAndPublisherIdentityForStorage() {
        CreateStudentJobPostingRequest req = new CreateStudentJobPostingRequest();
        req.setSubjectId(201L);
        req.setSubjectName("数学");
        req.setTitle("初中数学一对一");
        req.setDescription("描述");
        req.setStudentGender("female");
        req.setGradeCode("JUNIOR1");
        req.setClassMode("online");
        req.setFrequencyPerWeek(2);
        req.setStageCode("JUNIOR");
        req.setEducationRequirement("unlimited");
        req.setPublisherIdentity("parent");
        req.setBudgetMin(new BigDecimal("100"));
        req.setBudgetMax(new BigDecimal("200"));

        PositionPost post = new PositionPost();
        post.setId(201L);
        post.setName("数学");
        when(positionPostMapper.selectByIds(anyList())).thenReturn(java.util.List.of(post));

        doAnswer(inv -> {
            StudentJobPosting p = inv.getArgument(0);
            p.setId(3001L);
            return 1;
        }).when(studentJobPostingMapper).insert(any(StudentJobPosting.class));

        Long id = service.create(req, 101L);
        assertThat(id).isEqualTo(3001L);

        ArgumentCaptor<StudentJobPosting> captor = ArgumentCaptor.forClass(StudentJobPosting.class);
        verify(studentJobPostingMapper, times(1)).insert(captor.capture());
        StudentJobPosting saved = captor.getValue();
        assertThat(saved.getEducationRequirement()).isEqualTo("UNLIMITED");
        assertThat(saved.getPublisherIdentity()).isEqualTo("PARENT");
        assertThat(saved.getFrequencyPerWeek()).isEqualTo(2);
    }

    @Test
    void listPublishedShouldIgnoreUnlimitedEducationRequirementFilter() {
        CursorPageRequest page = new CursorPageRequest();
        page.setCursor(null);
        page.setPageSize(10);

        when(studentJobPostingMapper.listPublishedFiltered(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(java.util.List.of());

        service.listPublished(
                null,
                null,
                null,
                "北京",
                "offline",
                "JUNIOR",
                2,
                "UNLIMITED",
                null,
                null,
                null,
                null,
                "latest",
                page
        );

        verify(studentJobPostingMapper, times(1)).listPublishedFiltered(
                isNull(),
                isNull(),
                isNull(),
                eq("北京"),
                eq("offline"),
                eq("JUNIOR"),
                eq(2),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("latest"),
                isNull(),
                eq(10)
        );
    }


    @Test
    void getViewByIdShouldIncludePublisherSummary() {
        StudentJobPosting posting = StudentJobPosting.builder()
                .id(3001L)
                .parentId(101L)
                .subjectId(201L)
                .title("初中数学一对一")
                .description("描述")
                .classMode("online")
                .frequencyPerWeek(2)
                .stageCode("JUNIOR")
                .educationRequirement("BACHELOR")
                .publisherIdentity("STUDENT_SELF")
                .status(1)
                .build();
        when(studentJobPostingMapper.selectById(3001L)).thenReturn(posting);

        User u = new User();
        u.setId(101L);
        u.setName("学生-陈同学");
        u.setAvatar("/avatars/u101.png");
        when(userMapper.selectById(101L)).thenReturn(u);

        DemandViewVO vo = service.getViewById(3001L);
        assertThat(vo.getId()).isEqualTo(3001L);
        assertThat(vo.getPublisher()).isNotNull();
        assertThat(vo.getPublisher().getDisplayName()).isEqualTo("学生-陈同学");
        assertThat(vo.getPublisher().getAvatar()).isEqualTo("/avatars/u101.png");
        assertThat(vo.getPublisher().getIdentityLabel()).isEqualTo("学生本人");
    }
}
