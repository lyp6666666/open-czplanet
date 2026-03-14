package com.ai.tutor.appointment.model.vo;

import com.ai.tutor.appointment.model.entity.OrganizationProfile;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class OrgLoginVO implements Serializable {

    private Long id;

    private String name;

    private Integer userType;

    private String token;

    private Boolean mustChangePassword;

    private OrganizationProfile organizationProfile;

    private static final long serialVersionUID = 1L;
}
