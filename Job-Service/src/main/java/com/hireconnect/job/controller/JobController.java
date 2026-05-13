package com.hireconnect.job.controller;

import com.hireconnect.job.dto.request.JobRequest;
import org.springframework.security.core.Authentication;
import com.hireconnect.job.dto.response.ApiResponse;
import com.hireconnect.job.dto.response.JobResponse;
import com.hireconnect.job.enums.JobStatus;
import com.hireconnect.job.enums.JobType;
import com.hireconnect.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Jobs", description = "Job posting creation, search, filtering and lifecycle management")
public class JobController {

	private final JobService jobService;

	// ─── Public Endpoints (no auth required) ──────────────────────────────────

	@Operation(summary = "Browse all active jobs (public)", description = "Returns a paginated list of all ACTIVE job postings. No authentication required.")
	@GetMapping
	public ResponseEntity<ApiResponse<Page<JobResponse>>> getAllActiveJobs(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "postedAt") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir) {
		Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
		Pageable pageable = PageRequest.of(page, size, sort);
		return ResponseEntity.ok(ApiResponse.success(jobService.getAllActiveJobs(pageable)));
	}

	@Operation(summary = "Search and filter jobs (public)", description = "Full-text search on title/location/category with salary and experience filters.")
	@GetMapping("/search")
	public ResponseEntity<ApiResponse<Page<JobResponse>>> searchJobs(@RequestParam(required = false) String title,
			@RequestParam(required = false) String location, @RequestParam(required = false) String category,
			@RequestParam(required = false) JobType jobType, @RequestParam(required = false) Integer experience,
			@RequestParam(required = false) Double minSalary, @RequestParam(required = false) Double maxSalary,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("postedAt").descending());
		Page<JobResponse> result = jobService.searchJobs(title, location, category, jobType, experience, minSalary,
				maxSalary, pageable);
		return ResponseEntity.ok(ApiResponse.success(result));
	}

	@Operation(summary = "Get all jobs for admin", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<JobResponse>>> getAllJobsForAdmin(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir, @RequestParam(required = false) JobStatus status) {
		Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
		Pageable pageable = PageRequest.of(page, size, sort);
		return ResponseEntity.ok(ApiResponse.success(jobService.getAllJobs(pageable, status)));
	}

	@Operation(summary = "Get job by ID (public)", description = "Returns full job details and increments view count.")
	@GetMapping("/{jobId}")
	public ResponseEntity<ApiResponse<JobResponse>> getJobById(@PathVariable Long jobId,
			@Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId,
			@Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String userRole) {
		JobResponse response = jobService.getJobById(jobId, userId, userRole);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	// ─── Recruiter Endpoints (auth required) ──────────────────────────────────

	@Operation(summary = "Post a new job", description = "Creates a new ACTIVE job posting.", security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Job created"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not a recruiter") })

	@PostMapping
	@PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
	public ResponseEntity<ApiResponse<JobResponse>> createJob(@Valid @RequestBody JobRequest request,
			Authentication authentication) {

		Long recruiterId = Long.parseLong(authentication.getName());

		log.info("POST /jobs - recruiterId={}", recruiterId);

		JobResponse response = jobService.createJob(recruiterId, null, request);

		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Job posted successfully", response));
	}

	@Operation(summary = "Get recruiter's job postings", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/recruiter")
	@PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
	public ResponseEntity<ApiResponse<Page<JobResponse>>> getJobsByRecruiter(Authentication authentication,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) JobStatus status) {

		Long recruiterId = Long.parseLong(authentication.getName());

		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

		Page<JobResponse> result = status != null
				? jobService.getJobsByRecruiterAndStatus(recruiterId, status, pageable)
				: jobService.getJobsByRecruiter(recruiterId, pageable);

		return ResponseEntity.ok(ApiResponse.success(result));
	}

	@Operation(summary = "Get all jobs by recruiter (no pagination)", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/recruiter/{recruiterId}/all")
	@PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
	public ResponseEntity<ApiResponse<List<JobResponse>>> getAllJobsByRecruiter(@PathVariable Long recruiterId) {
		return ResponseEntity.ok(ApiResponse.success(jobService.getAllJobsByRecruiter(recruiterId)));
	}

	@Operation(summary = "Update a job posting", security = @SecurityRequirement(name = "bearerAuth"))
	@PutMapping("/{jobId}")
	@PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
	public ResponseEntity<ApiResponse<JobResponse>> updateJob(@PathVariable Long jobId,
			@Valid @RequestBody JobRequest request, Authentication authentication) {
		Long recruiterId = Long.parseLong(authentication.getName());
		JobResponse response = jobService.updateJob(jobId, recruiterId, request);
		return ResponseEntity.ok(ApiResponse.success("Job updated successfully", response));
	}

	@Operation(summary = "Update job status (ACTIVE/PAUSED/CLOSED/DRAFT)", security = @SecurityRequirement(name = "bearerAuth"))
	@PatchMapping("/{jobId}/status")
	@PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
	public ResponseEntity<ApiResponse<JobResponse>> updateJobStatus(@PathVariable Long jobId,
			@RequestParam JobStatus status, @Parameter(hidden = true) Authentication authentication) {
		JobResponse response = isAdmin(authentication)
				? jobService.updateJobStatusAsAdmin(jobId, status)
				: jobService.updateJobStatus(jobId, Long.parseLong(authentication.getName()), status);
		return ResponseEntity.ok(ApiResponse.success("Job status updated to " + status, response));
	}

	@PatchMapping("/{jobId}/close")
	@PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
	public ResponseEntity<ApiResponse<JobResponse>> closeJob(@PathVariable Long jobId, Authentication authentication) {

		JobResponse response = isAdmin(authentication)
				? jobService.updateJobStatusAsAdmin(jobId, JobStatus.CLOSED)
				: jobService.updateJobStatus(jobId, Long.parseLong(authentication.getName()), JobStatus.CLOSED);

		return ResponseEntity.ok(ApiResponse.success("Job closed successfully", response));
	}

	@PatchMapping("/{jobId}/pause")
	@PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
	public ResponseEntity<ApiResponse<JobResponse>> pauseJob(@PathVariable Long jobId, Authentication authentication) {

		JobResponse response = isAdmin(authentication)
				? jobService.updateJobStatusAsAdmin(jobId, JobStatus.PAUSED)
				: jobService.updateJobStatus(jobId, Long.parseLong(authentication.getName()), JobStatus.PAUSED);

		return ResponseEntity.ok(ApiResponse.success("Job paused successfully", response));
	}

	@Operation(summary = "Delete a job posting", security = @SecurityRequirement(name = "bearerAuth"))
	@DeleteMapping("/{jobId}")
	@PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
	public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long jobId,
			@Parameter(hidden = true) Authentication authentication) {
		if (isAdmin(authentication)) {
			jobService.deleteJobAsAdmin(jobId);
		} else {
			jobService.deleteJob(jobId, Long.parseLong(authentication.getName()));
		}
		return ResponseEntity.ok(ApiResponse.success("Job deleted successfully", null));
	}

	private boolean isAdmin(Authentication authentication) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
	}

	// ─── Internal endpoints (called by other services) ─────────────────────────

	@Operation(summary = "Check if job exists (internal)", description = "Used by application-service to validate job.")
	@GetMapping("/{jobId}/exists")
	public ResponseEntity<ApiResponse<Boolean>> jobExists(@PathVariable Long jobId) {
		return ResponseEntity.ok(ApiResponse.success(jobService.jobExists(jobId)));
	}

	@Operation(summary = "Get recruiter ID by job (internal)")
	@GetMapping("/{jobId}/recruiter")
	public ResponseEntity<ApiResponse<Long>> getRecruiterByJob(@PathVariable Long jobId) {
		return ResponseEntity.ok(ApiResponse.success(jobService.getRecruiterIdByJob(jobId)));
	}

	@Operation(summary = "Count jobs by recruiter (internal)")
	@GetMapping("/recruiter/{recruiterId}/count")
	public ResponseEntity<ApiResponse<Long>> countJobsByRecruiter(@PathVariable Long recruiterId) {
		return ResponseEntity.ok(ApiResponse.success(jobService.countJobsByRecruiter(recruiterId)));
	}
}
