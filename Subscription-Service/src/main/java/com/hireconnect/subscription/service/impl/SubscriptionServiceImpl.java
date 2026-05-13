package com.hireconnect.subscription.service.impl;

import com.hireconnect.subscription.dto.request.SubscribeRequest;
import com.hireconnect.subscription.dto.response.InvoiceResponse;
import com.hireconnect.subscription.dto.request.RazorpayOrderRequest;
import com.hireconnect.subscription.dto.request.RazorpayVerifyRequest;
import com.hireconnect.subscription.dto.response.RazorpayOrderResponse;
import com.hireconnect.subscription.dto.response.SubscriptionPlanResponse;
import com.hireconnect.subscription.dto.response.SubscriptionResponse;
import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.entity.Subscription;
import com.hireconnect.subscription.enums.PaymentMode;
import com.hireconnect.subscription.enums.SubscriptionPlan;
import com.hireconnect.subscription.enums.SubscriptionStatus;
import com.hireconnect.subscription.exception.ResourceNotFoundException;
import com.hireconnect.subscription.mapper.SubscriptionMapper;
import com.hireconnect.subscription.repository.InvoiceRepository;
import com.hireconnect.subscription.repository.SubscriptionRepository;
import com.hireconnect.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.payment.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${app.payment.razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.notification}")
    private String notificationRoutingKey;

    @Value("${app.services.auth-url:http://localhost:8081/api/v1/auth}")
    private String authServiceUrl;

    // Plan configs
    private static final int FREE_MAX_POSTS         = 3;
    private static final int PROFESSIONAL_MAX_POSTS  = 50;
    private static final int ENTERPRISE_MAX_POSTS    = 999;
    private static final double PROFESSIONAL_PRICE   = 1999.0;
    private static final double ENTERPRISE_PRICE     = 4999.0;
    private static final double GST_RATE             = 0.18;
    private static final int PLAN_DURATION_DAYS      = 30;

    @Override
    public SubscriptionResponse subscribe(Long recruiterId, SubscribeRequest request) {
        log.info("Recruiter {} subscribing to plan: {}", recruiterId, request.getPlan());

        // Cancel any existing active subscription
        subscriptionRepository.findActiveByRecruiterId(recruiterId, LocalDate.now())
                .ifPresent(existing -> {
                    existing.setStatus(SubscriptionStatus.CANCELLED);
                    subscriptionRepository.save(existing);
                    log.info("Cancelled existing subscription {} for recruiter {}", existing.getSubscriptionId(), recruiterId);
                });

        double amount = getPlanPrice(request.getPlan());
        int maxPosts  = getMaxPosts(request.getPlan());
        LocalDate startDate = LocalDate.now();
        LocalDate endDate   = request.getPlan() == SubscriptionPlan.FREE ? null : startDate.plusDays(PLAN_DURATION_DAYS);

        Subscription subscription = Subscription.builder()
                .recruiterId(recruiterId)
                .plan(request.getPlan())
                .startDate(startDate)
                .endDate(endDate)
                .status(SubscriptionStatus.ACTIVE)
                .amountPaid(amount)
                .maxJobPosts(maxPosts)
                .autoRenew(request.getAutoRenew())
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription {} created for recruiter {}", saved.getSubscriptionId(), recruiterId);

        // Generate invoice for paid plans
        Invoice invoice = amount > 0 ? generateInvoiceInternal(saved, request) : null;
        publishSubscriptionPurchasedNotification(saved, invoice, request.getPaymentMode());

        return subscriptionMapper.toResponse(saved);
    }

    @Override
    public SubscriptionResponse cancelSubscription(Long recruiterId) {
        Subscription subscription = subscriptionRepository
                .findActiveByRecruiterId(recruiterId, LocalDate.now())
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for recruiter: " + recruiterId));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setAutoRenew(false);
        Subscription updated = subscriptionRepository.save(subscription);
        log.info("Subscription {} cancelled for recruiter {}", subscription.getSubscriptionId(), recruiterId);

        return subscriptionMapper.toResponse(updated);
    }

    @Override
    public SubscriptionResponse renewSubscription(Long recruiterId, SubscribeRequest request) {
        log.info("Renewing subscription for recruiter {}", recruiterId);
        return subscribe(recruiterId, request);
    }

    @Override
    public RazorpayOrderResponse createRazorpayOrder(Long recruiterId, RazorpayOrderRequest request) {
        if (request.getPlan() == SubscriptionPlan.FREE) {
            throw new IllegalStateException("Free plan does not need payment");
        }
        ensureRazorpayConfigured();

        double baseAmount = getPlanPrice(request.getPlan());
        double gstAmount = baseAmount * GST_RATE;
        double total = baseAmount + gstAmount;
        int amountPaise = (int) Math.round(total * 100);
        String receipt = "HC-" + recruiterId + "-" + System.currentTimeMillis();

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(razorpayKeyId, razorpayKeySecret, StandardCharsets.UTF_8);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "amount", amountPaise,
                "currency", "INR",
                "receipt", receipt,
                "payment_capture", 1,
                "notes", Map.of(
                        "recruiterId", recruiterId.toString(),
                        "plan", request.getPlan().name()
                )
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.razorpay.com/v1/orders",
                new HttpEntity<>(body, headers),
                Map.class
        );

        Map<?, ?> data = response.getBody();
        if (data == null || data.get("id") == null) {
            throw new IllegalStateException("Unable to create Razorpay order");
        }

        return RazorpayOrderResponse.builder()
                .keyId(razorpayKeyId)
                .orderId(String.valueOf(data.get("id")))
                .currency("INR")
                .amount(amountPaise)
                .amountRupees(total)
                .gstAmount(gstAmount)
                .totalAmount(total)
                .plan(request.getPlan())
                .receipt(receipt)
                .build();
    }

    @Override
    public SubscriptionResponse verifyRazorpayPayment(Long recruiterId, RazorpayVerifyRequest request) {
        if (request.getPlan() == SubscriptionPlan.FREE) {
            throw new IllegalStateException("Free plan does not need payment verification");
        }
        ensureRazorpayConfigured();
        if (!isValidRazorpaySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature())) {
            throw new IllegalStateException("Payment verification failed");
        }

        SubscribeRequest subscribeRequest = new SubscribeRequest();
        subscribeRequest.setPlan(request.getPlan());
        subscribeRequest.setPaymentMode(request.getPaymentMode());
        subscribeRequest.setAutoRenew(request.getAutoRenew());
        subscribeRequest.setRazorpayOrderId(request.getRazorpayOrderId());
        subscribeRequest.setRazorpayPaymentId(request.getRazorpayPaymentId());
        subscribeRequest.setRazorpaySignature(request.getRazorpaySignature());
        return subscribe(recruiterId, subscribeRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getPlans() {
        return List.of(
                planResponse(SubscriptionPlan.FREE,
                        "Free",
                        List.of("3 job posts", "Basic candidate management", "Standard notifications")),
                planResponse(SubscriptionPlan.PROFESSIONAL,
                        "Professional",
                        List.of("50 job posts", "Recruiter analytics", "Shortlisted candidate messaging", "Invoice history")),
                planResponse(SubscriptionPlan.ENTERPRISE,
                        "Enterprise",
                        List.of("999 job posts", "Advanced analytics", "Priority support", "Full billing dashboard"))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getActiveSubscription(Long recruiterId) {
        Subscription subscription = subscriptionRepository
                .findActiveByRecruiterId(recruiterId, LocalDate.now())
                .orElseGet(() -> Subscription.builder()
                        .recruiterId(recruiterId)
                        .plan(SubscriptionPlan.FREE)
                        .status(SubscriptionStatus.ACTIVE)
                        .amountPaid(0.0)
                        .maxJobPosts(FREE_MAX_POSTS)
                        .build());
        return subscriptionMapper.toResponse(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getAllSubscriptionsByRecruiter(Long recruiterId) {
        return subscriptionRepository.findByRecruiterId(recruiterId)
                .stream().map(subscriptionMapper::toResponse).toList();
    }

    @Override
    public InvoiceResponse generateInvoice(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", subscriptionId));
        return subscriptionMapper.toInvoiceResponse(
                invoiceRepository.findFirstBySubscriptionIdOrderByCreatedAtDesc(subscriptionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for subscription: " + subscriptionId))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getInvoicesByRecruiter(Long recruiterId, Pageable pageable) {
        return invoiceRepository.findByRecruiterIdOrderByCreatedAtDesc(recruiterId, pageable)
                .map(subscriptionMapper::toInvoiceResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoicesByRecruiter(Long recruiterId) {
        return invoiceRepository.findByRecruiterIdOrderByCreatedAtDesc(recruiterId)
                .stream().map(subscriptionMapper::toInvoiceResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        return subscriptionMapper.toInvoiceResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(Long recruiterId) {
        return subscriptionRepository.findActiveByRecruiterId(recruiterId, LocalDate.now()).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public int getMaxJobPosts(Long recruiterId) {
        return subscriptionRepository.findActiveByRecruiterId(recruiterId, LocalDate.now())
                .map(s -> s.getMaxJobPosts() != null ? s.getMaxJobPosts() : FREE_MAX_POSTS)
                .orElse(FREE_MAX_POSTS);
    }

    // ---- private helpers ----

    private Invoice generateInvoiceInternal(Subscription subscription, SubscribeRequest request) {
        double baseAmount = subscription.getAmountPaid();
        double gstAmount  = baseAmount * GST_RATE;
        double total      = baseAmount + gstAmount;

        Invoice invoice = Invoice.builder()
                .subscriptionId(subscription.getSubscriptionId())
                .recruiterId(subscription.getRecruiterId())
                .amount(baseAmount)
                .gstAmount(gstAmount)
                .totalAmount(total)
                .paymentDate(LocalDateTime.now())
                .paymentMode(request.getPaymentMode())
                .transactionId(resolveTransactionId(request))
                .razorpayOrderId(request.getRazorpayOrderId())
                .razorpayPaymentId(request.getRazorpayPaymentId())
                .invoiceNumber(generateInvoiceNumber())
                .planName(subscription.getPlan().name())
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice {} generated for subscription {}", invoice.getInvoiceNumber(), subscription.getSubscriptionId());
        return saved;
    }

    private void publishSubscriptionPurchasedNotification(Subscription subscription, Invoice invoice, PaymentMode paymentMode) {
        try {
            List<Long> adminIds = fetchActiveAdminIds();
            if (adminIds.isEmpty()) {
                log.warn("No active admins found while publishing subscription purchase notification; publishing role-targeted event");
            }

            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "SUBSCRIPTION_PURCHASED");
            event.put("targetRole", "ADMIN");
            event.put("adminIds", adminIds);
            event.put("recruiterId", subscription.getRecruiterId());
            event.put("subscriptionId", subscription.getSubscriptionId());
            event.put("plan", subscription.getPlan().name());
            event.put("amount", invoice != null ? invoice.getTotalAmount() : subscription.getAmountPaid());
            event.put("paymentMode", invoice != null && invoice.getPaymentMode() != null
                    ? invoice.getPaymentMode().name()
                    : paymentMode != null ? paymentMode.name() : "");
            event.put("invoiceId", invoice != null ? invoice.getInvoiceId() : "");
            event.put("invoiceNumber", invoice != null ? invoice.getInvoiceNumber() : "");

            rabbitTemplate.convertAndSend(exchange, notificationRoutingKey, event);
            log.info("Published SUBSCRIPTION_PURCHASED notification for subscription {} to {} admins",
                    subscription.getSubscriptionId(), adminIds.size());
        } catch (Exception e) {
            log.warn("Failed to publish subscription purchase notification: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> fetchActiveAdminIds() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(authServiceUrl + "/internal/admin-ids", Map.class);
            Object data = response.getBody() != null ? response.getBody().get("data") : null;
            if (!(data instanceof List<?> values)) {
                return List.of();
            }
            return values.stream()
                    .map(value -> value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value)))
                    .toList();
        } catch (Exception e) {
            log.warn("Unable to fetch active admin ids: {}", e.getMessage());
            return List.of();
        }
    }

    private String generateInvoiceNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "HC-INV-" + date + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String resolveTransactionId(SubscribeRequest request) {
        if (request.getRazorpayPaymentId() != null && !request.getRazorpayPaymentId().isBlank()) {
            return request.getRazorpayPaymentId();
        }
        if (request.getPaymentMode() == PaymentMode.WALLET && request.getPaymentToken() != null
                && !request.getPaymentToken().isBlank()) {
            return request.getPaymentToken();
        }
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private SubscriptionPlanResponse planResponse(SubscriptionPlan plan, String name, List<String> features) {
        double price = getPlanPrice(plan);
        double gstAmount = price * GST_RATE;
        return SubscriptionPlanResponse.builder()
                .plan(plan)
                .name(name)
                .price(price)
                .gstAmount(gstAmount)
                .totalAmount(price + gstAmount)
                .maxJobPosts(getMaxPosts(plan))
                .durationDays(plan == SubscriptionPlan.FREE ? null : PLAN_DURATION_DAYS)
                .features(features)
                .build();
    }

    private void ensureRazorpayConfigured() {
        if (razorpayKeyId == null || razorpayKeyId.isBlank()
                || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new IllegalStateException("Razorpay credentials are not configured");
        }
    }

    private boolean isValidRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String generated = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(
                    generated.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.warn("Razorpay signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private double getPlanPrice(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE         -> 0.0;
            case PROFESSIONAL -> PROFESSIONAL_PRICE;
            case ENTERPRISE   -> ENTERPRISE_PRICE;
        };
    }

    private int getMaxPosts(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE         -> FREE_MAX_POSTS;
            case PROFESSIONAL -> PROFESSIONAL_MAX_POSTS;
            case ENTERPRISE   -> ENTERPRISE_MAX_POSTS;
        };
    }
}
