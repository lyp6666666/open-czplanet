package com.ai.tutor.admin.service;

import com.ai.tutor.admin.model.dto.AdminCustomerServiceConfigRequest;
import com.ai.tutor.admin.model.vo.AdminCustomerServiceConfigVO;
import org.springframework.web.multipart.MultipartFile;

public interface AdminCustomerServiceConfigService {
    AdminCustomerServiceConfigVO config();

    AdminCustomerServiceConfigVO save(AdminCustomerServiceConfigRequest request, Long adminUid);

    AdminCustomerServiceConfigVO uploadQrCode(MultipartFile file, Long adminUid);
}
