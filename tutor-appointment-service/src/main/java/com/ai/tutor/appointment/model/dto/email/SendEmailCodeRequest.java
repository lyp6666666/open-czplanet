package com.ai.tutor.appointment.model.dto.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendEmailCodeRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String emailType;

    private String scene;
}
