package com.hireconnect.subscription.controller;

import com.hireconnect.subscription.dto.request.SubscribeRequest;
import com.hireconnect.subscription.dto.request.RazorpayOrderRequest;
import com.hireconnect.subscription.dto.request.RazorpayVerifyRequest;
import com.hireconnect.subscription.dto.response.ApiResponse;
import com.hireconnect.subscription.dto.response.InvoiceResponse;
import com.hireconnect.subscription.dto.response.RazorpayOrderResponse;
import com.hireconnect.subscription.dto.response.SubscriptionPlanResponse;
import com.hireconnect.subscription.dto.response.SubscriptionResponse;
import com.hireconnect.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /** Subscribe to a plan */
    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> subscribe(
            @Valid @RequestBody SubscribeRequest request,
            @RequestHeader("X-User-Id") Long recruiterId) {
        log.info("POST /subscriptions - recruiter={}, plan={}", recruiterId, request.getPlan());
        SubscriptionResponse response = subscriptionService.subscribe(recruiterId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Subscribed successfully to " + request.getPlan() + " plan", response));
    }

    @GetMapping("/plans")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> getPlans() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getPlans()));
    }

    @PostMapping("/payments/razorpay/order")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> createRazorpayOrder(
            @Valid @RequestBody RazorpayOrderRequest request,
            @RequestHeader("X-User-Id") Long recruiterId) {
        RazorpayOrderResponse response = subscriptionService.createRazorpayOrder(recruiterId, request);
        return ResponseEntity.ok(ApiResponse.success("Razorpay order created", response));
    }

    @PostMapping("/payments/razorpay/verify")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> verifyRazorpayPayment(
            @Valid @RequestBody RazorpayVerifyRequest request,
            @RequestHeader("X-User-Id") Long recruiterId) {
        SubscriptionResponse response = subscriptionService.verifyRazorpayPayment(recruiterId, request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified and subscription activated", response));
    }

    /** Get active subscription */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getActiveSubscription(
            @RequestHeader("X-User-Id") Long recruiterId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getActiveSubscription(recruiterId)));
    }

    /** Get active subscription for a specific recruiter (Admin or self) */
    @GetMapping("/recruiter/{recruiterId}/active")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getActiveSubscriptionByRecruiter(
            @PathVariable Long recruiterId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getActiveSubscription(recruiterId)));
    }

    /** Get all subscriptions for current recruiter */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getSubscriptionHistory(
            @RequestHeader("X-User-Id") Long recruiterId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getAllSubscriptionsByRecruiter(recruiterId)));
    }

    /** Cancel current active subscription */
    @DeleteMapping("/cancel")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancelSubscription(
            @RequestHeader("X-User-Id") Long recruiterId) {
        SubscriptionResponse response = subscriptionService.cancelSubscription(recruiterId);
        return ResponseEntity.ok(ApiResponse.success("Subscription cancelled", response));
    }

    /** Renew / upgrade plan */
    @PutMapping("/renew")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> renewSubscription(
            @Valid @RequestBody SubscribeRequest request,
            @RequestHeader("X-User-Id") Long recruiterId) {
        SubscriptionResponse response = subscriptionService.renewSubscription(recruiterId, request);
        return ResponseEntity.ok(ApiResponse.success("Subscription renewed successfully", response));
    }

    /** Check if recruiter has active subscription */
    @GetMapping("/check")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> hasActiveSubscription(
            @RequestParam Long recruiterId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.hasActiveSubscription(recruiterId)));
    }

    /** Get max job posts allowed */
    @GetMapping("/max-posts")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> getMaxJobPosts(
            @RequestParam Long recruiterId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getMaxJobPosts(recruiterId)));
    }

    @GetMapping("/internal/recruiters/{recruiterId}/max-posts")
    public ResponseEntity<ApiResponse<Integer>> getInternalMaxJobPosts(@PathVariable Long recruiterId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getMaxJobPosts(recruiterId)));
    }

    // ---- Invoice endpoints ----

    /** Get invoices (paginated) */
    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getInvoices(
            @RequestHeader("X-User-Id") Long recruiterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getInvoicesByRecruiter(recruiterId, pageable)));
    }

    /** Get all invoices for a recruiter */
    @GetMapping("/recruiter/{recruiterId}/invoices")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getAllInvoices(@PathVariable Long recruiterId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getAllInvoicesByRecruiter(recruiterId)));
    }

    /** Get invoice by ID */
    @GetMapping("/invoices/{invoiceId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getInvoiceById(invoiceId)));
    }
}
