package com.ai.tutor.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class AdminOrganizationCreateResponse implements Serializable {

    private Long orgUserId;

    private String username;

    private String initialPassword;

    private static final long serialVersionUID = 1L;
}
