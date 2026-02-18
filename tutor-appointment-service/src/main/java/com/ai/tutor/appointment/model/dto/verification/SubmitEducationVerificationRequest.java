package com.ai.tutor.appointment.model.dto.verification;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SubmitEducationVerificationRequest {
    @NotEmpty
    private List<String> proofUrls;
}

