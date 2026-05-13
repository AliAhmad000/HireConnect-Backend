package com.hireconnect.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateNotificationRecipientResponse {
    private Long userId;
    private String fullName;
    private String email;
}
