package com.hireconnect.profile.dto.response;
import lombok.Builder; import lombok.Data;
import java.time.LocalDate; import java.time.LocalDateTime; import java.util.List;
@Data @Builder
public class CandidateProfileResponse {
    private Long profileId; private Long userId; private String fullName; private String email;
    private String mobile; private LocalDate dob; private String gender;
    private List<String> skills; private Integer experience; private String resumeUrl;
    private String linkedinUrl; private String githubUrl; private String portfolioUrl;
    private String summary; private String currentCompany; private String currentDesignation;
    private Double expectedSalary; private Integer noticePeriodDays; private Boolean isOpenToRemote;
    private List<String> preferredLocations; private List<AddressResponse> addresses;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
