package com.ai.tutor.appointment.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorFavoriteDemand {

    private Long id;

    private Long tutorId;

    private Long demandId;

    private LocalDateTime createTime;
}

