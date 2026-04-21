package com.ai.tutor.admin.service;

import com.ai.tutor.admin.mapper.AdminVerificationMapper;
import com.ai.tutor.admin.model.entity.TeacherProfile;
import com.ai.tutor.admin.service.impl.AdminVerificationServiceImpl;
import com.ai.tutor.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminVerificationServiceImplTest {

    @Mock
    private AdminVerificationMapper adminVerificationMapper;

    @InjectMocks
    private AdminVerificationServiceImpl service;

    @Test
    void approveRealnameShouldRejectLegacyNoPhotoSubmission() {
        TeacherProfile profile = new TeacherProfile();
        profile.setUserId(1001L);
        profile.setRealnameVerifyStatus(1);
        profile.setRealnameVerifyMethod("NAME_IDNO");
        when(adminVerificationMapper.selectByUserId(1001L)).thenReturn(profile);

        assertThrows(BusinessException.class, () -> service.approveVerification(1001L, "REALNAME"));

        verify(adminVerificationMapper, never()).approveRealname(1001L);
    }

    @Test
    void approveEduShouldRequireProofImages() {
        TeacherProfile profile = new TeacherProfile();
        profile.setUserId(1002L);
        profile.setEduVerifyStatus(1);
        profile.setEduVerifyProofUrls(" ");
        when(adminVerificationMapper.selectByUserId(1002L)).thenReturn(profile);

        assertThrows(BusinessException.class, () -> service.approveVerification(1002L, "EDU"));

        verify(adminVerificationMapper, never()).approveEdu(1002L);
    }

    @Test
    void approveRealnameShouldPassWhenPhotoEvidenceReady() {
        TeacherProfile profile = new TeacherProfile();
        profile.setUserId(1003L);
        profile.setRealnameVerifyStatus(1);
        profile.setRealnameVerifyMethod("ID_PHOTO");
        profile.setRealnameVerifyIdFrontUrl("https://img/front.png");
        profile.setRealnameVerifyIdBackUrl("https://img/back.png");
        when(adminVerificationMapper.selectByUserId(1003L)).thenReturn(profile);
        when(adminVerificationMapper.approveRealname(1003L)).thenReturn(1);

        service.approveVerification(1003L, "REALNAME");

        verify(adminVerificationMapper).approveRealname(1003L);
    }
}
