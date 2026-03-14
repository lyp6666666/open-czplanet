package com.ai.tutor.appointment.model.vo.parent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class ParentTutorVOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TutorCardVO {
        private Long userId;
        private String displayName;
        private String avatar;
        private String city;
        private String education;
        private Integer experienceYears;
        private String ratePerHour;
        private String teachingMode;
        private List<String> subjectTags;
        private List<String> highlights;
        private String introduction;
    }
}
