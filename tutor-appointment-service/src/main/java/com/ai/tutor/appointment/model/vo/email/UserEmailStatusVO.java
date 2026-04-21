package com.ai.tutor.appointment.model.vo.email;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserEmailStatusVO {
    private UserEmailItemVO primaryEmail;
    private UserEmailItemVO summaryEmail;
    private Boolean canUseSummaryEmail;
    private Tips tips;

    @Data
    @Builder
    public static class Tips {
        private Boolean primaryEmailMissing;
        private Boolean summaryEmailMissing;
    }
}
