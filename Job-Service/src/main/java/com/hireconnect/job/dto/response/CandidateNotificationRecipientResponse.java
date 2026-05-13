package com.hireconnect.job.dto.response;

import lombok.Data;

@Data
public class CandidateNotificationRecipientResponse {
    private Long userId;
    private String fullName;
    private String email;
}
