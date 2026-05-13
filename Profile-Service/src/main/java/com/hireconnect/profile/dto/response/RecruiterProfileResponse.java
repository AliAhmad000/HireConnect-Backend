package com.hireconnect.profile.dto.response;
import lombok.Builder; import lombok.Data; import java.time.LocalDateTime;
@Data @Builder
public class RecruiterProfileResponse {
    private Long profileId; private Long userId; private String fullName; private String email;
    private String mobile; private String companyName; private String companySize;
    private String industry; private String website; private String companyDescription;
    private String linkedinUrl; private String logoUrl; private String designation;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
