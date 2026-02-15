package com.ai.tutor.appointment.model.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class UserSimpleVO implements Serializable {

    private Long id;

    private String name;

    private String avatar;

    private Integer userType;

    private static final long serialVersionUID = 1L;
}

