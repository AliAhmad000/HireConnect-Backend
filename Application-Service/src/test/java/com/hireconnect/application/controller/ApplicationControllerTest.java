package com.hireconnect.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireconnect.application.dto.request.RecruiterMessageRequest;
import com.hireconnect.application.dto.request.SubmitApplicationRequest;
import com.hireconnect.application.dto.response.ApplicationResponse;
import com.hireconnect.application.enums.ApplicationStatus;
import com.hireconnect.application.exception.DuplicateApplicationException;
import com.hireconnect.application.service.ApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
@Import(ApplicationControllerTest.MethodSecurityConfig.class)
@DisplayName("ApplicationController Tests")
class ApplicationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private ApplicationService applicationService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    private ApplicationResponse sampleResponse() {
        return ApplicationResponse.builder()
                .applicationId(1L)
                .jobId(10L)
                .candidateId(1L)
                .recruiterId(2L)
                .jobTitle("Software Engineer")
                .companyName("TechCorp")
                .candidateName("John Doe")
                .candidateEmail("john@example.com")
                .status(ApplicationStatus.APPLIED)
                .appliedAt(LocalDate.now())
                .isWithdrawn(false)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/applications - should submit application and return 201")
    @WithMockUser(roles = "CANDIDATE")
    void shouldSubmitApplicationAndReturn201() throws Exception {
        SubmitApplicationRequest request = new SubmitApplicationRequest();
        request.setJobId(10L);
        request.setRecruiterId(2L);
        request.setJobTitle("Software Engineer");
        request.setCompanyName("TechCorp");
        request.setResumeUrl("https://s3.example.com/resume.pdf");

        when(applicationService.submitApplication(anyLong(), anyString(), anyString(), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/applications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1")
                        .header("X-User-Name", "John Doe")
                        .header("X-User-Email", "john@example.com")
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicationId").value(1))
                .andExpect(jsonPath("$.data.status").value("APPLIED"));
    }

    @Test
    @DisplayName("POST /api/v1/applications - should return 409 on duplicate application")
    @WithMockUser(roles = "CANDIDATE")
    void shouldReturn409OnDuplicateApplication() throws Exception {
        SubmitApplicationRequest request = new SubmitApplicationRequest();
        request.setJobId(10L);
        request.setRecruiterId(2L);
        request.setJobTitle("Software Engineer");
        request.setCompanyName("TechCorp");
        request.setResumeUrl("https://s3.example.com/resume.pdf");

        when(applicationService.submitApplication(anyLong(), anyString(), anyString(), any()))
                .thenThrow(new DuplicateApplicationException(10L, 1L));

        mockMvc.perform(post("/api/v1/applications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1")
                        .header("X-User-Name", "John Doe")
                        .header("X-User-Email", "john@example.com")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/applications - should return 400 on validation failure")
    @WithMockUser(roles = "CANDIDATE")
    void shouldReturn400OnValidationFailure() throws Exception {
        SubmitApplicationRequest request = new SubmitApplicationRequest();
        // Missing required fields — should fail validation

        mockMvc.perform(post("/api/v1/applications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1")
                        .header("X-User-Name", "John Doe")
                        .header("X-User-Email", "john@example.com")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("GET /api/v1/applications/candidate/{id} - should return paginated applications")
    @WithMockUser(roles = "CANDIDATE")
    void shouldReturnPaginatedApplicationsForCandidate() throws Exception {
        Page<ApplicationResponse> page = new PageImpl<>(List.of(sampleResponse()));
        when(applicationService.getApplicationsByCandidate(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/applications/candidate/1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].applicationId").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/applications/check - should return true when applied")
    @WithMockUser(roles = "CANDIDATE")
    void shouldReturnTrueWhenCandidateHasApplied() throws Exception {
        when(applicationService.hasApplied(10L, 1L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/applications/check")
                        .param("jobId", "10")
                        .param("candidateId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/applications/{id}/messages - should send recruiter message")
    @WithMockUser(roles = "RECRUITER")
    void shouldSendRecruiterMessage() throws Exception {
        RecruiterMessageRequest request = new RecruiterMessageRequest();
        request.setMessage("Please share your availability for the next interview round.");

        mockMvc.perform(post("/api/v1/applications/4/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "2")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Message sent to candidate"));

        verify(applicationService).sendMessageToCandidate(eq(4L), any(RecruiterMessageRequest.class), eq(2L));
    }

    @Test
    @DisplayName("GET /api/v1/applications/candidate/{id} - should return 403 for wrong role")
    @WithMockUser(roles = "RECRUITER")
    void shouldReturn403ForWrongRole() throws Exception {
        mockMvc.perform(get("/api/v1/applications/candidate/1"))
                .andExpect(status().isForbidden());
    }
}
