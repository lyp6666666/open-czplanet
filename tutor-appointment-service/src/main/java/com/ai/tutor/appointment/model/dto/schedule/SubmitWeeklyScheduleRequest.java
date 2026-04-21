package com.ai.tutor.appointment.model.dto.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SubmitWeeklyScheduleRequest {
    @NotNull
    private Long participantUserId;
    private Long roomId;
    private String title;
    private String description;
    private Long lessonPriceFen;
    private Integer weeks;
    @Valid
    @NotEmpty
    private List<WeeklySlot> slots;

    @Data
    public static class WeeklySlot {
        @NotNull
        private Integer dayOfWeek;
        @NotNull
        private Integer startMinute;
        @NotNull
        private Integer endMinute;
    }
}
