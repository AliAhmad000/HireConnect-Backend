package com.hireconnect.subscription.service;

import com.hireconnect.subscription.dto.request.SubscribeRequest;
import com.hireconnect.subscription.dto.request.RazorpayOrderRequest;
import com.hireconnect.subscription.dto.request.RazorpayVerifyRequest;
import com.hireconnect.subscription.dto.response.InvoiceResponse;
import com.hireconnect.subscription.dto.response.RazorpayOrderResponse;
import com.hireconnect.subscription.dto.response.SubscriptionPlanResponse;
import com.hireconnect.subscription.dto.response.SubscriptionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SubscriptionService {

    SubscriptionResponse subscribe(Long recruiterId, SubscribeRequest request);

    SubscriptionResponse cancelSubscription(Long recruiterId);

    SubscriptionResponse renewSubscription(Long recruiterId, SubscribeRequest request);

    RazorpayOrderResponse createRazorpayOrder(Long recruiterId, RazorpayOrderRequest request);

    SubscriptionResponse verifyRazorpayPayment(Long recruiterId, RazorpayVerifyRequest request);

    List<SubscriptionPlanResponse> getPlans();

    SubscriptionResponse getActiveSubscription(Long recruiterId);

    List<SubscriptionResponse> getAllSubscriptionsByRecruiter(Long recruiterId);

    InvoiceResponse generateInvoice(Long subscriptionId);

    Page<InvoiceResponse> getInvoicesByRecruiter(Long recruiterId, Pageable pageable);

    List<InvoiceResponse> getAllInvoicesByRecruiter(Long recruiterId);

    InvoiceResponse getInvoiceById(Long invoiceId);

    boolean hasActiveSubscription(Long recruiterId);

    int getMaxJobPosts(Long recruiterId);
}
