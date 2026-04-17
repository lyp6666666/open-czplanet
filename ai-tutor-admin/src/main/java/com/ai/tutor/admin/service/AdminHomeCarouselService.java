package com.ai.tutor.admin.service;

import com.ai.tutor.admin.model.vo.AdminHomeCarouselItemVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminHomeCarouselService {
    List<AdminHomeCarouselItemVO> list();

    AdminHomeCarouselItemVO create(String title, String subtitle, String linkUrl, MultipartFile file, Long adminUid);

    void delete(Long id, Long adminUid);
}
