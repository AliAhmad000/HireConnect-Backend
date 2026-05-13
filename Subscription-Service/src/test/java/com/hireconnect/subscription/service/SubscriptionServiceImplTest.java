package com.hireconnect.subscription.service;

import com.hireconnect.subscription.dto.request.RazorpayOrderRequest;
import com.hireconnect.subscription.dto.request.RazorpayVerifyRequest;
import com.hireconnect.subscription.dto.request.SubscribeRequest;
import com.hireconnect.subscription.dto.response.InvoiceResponse;
import com.hireconnect.subscription.dto.response.RazorpayOrderResponse;
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
import com.hireconnect.subscription.service.impl.SubscriptionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionServiceImpl Tests")
class SubscriptionServiceImplTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private SubscriptionMapper subscriptionMapper;
    @Mock private RestTemplate restTemplate;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private SubscriptionServiceImpl subscriptionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionService, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(subscriptionService, "razorpayKeySecret", "secret");
        ReflectionTestUtils.setField(subscriptionService, "exchange", "hireconnect.exchange");
        ReflectionTestUtils.setField(subscriptionService, "notificationRoutingKey", "notification.routing.key");
        ReflectionTestUtils.setField(subscriptionService, "authServiceUrl", "http://auth-service/api/v1/auth");
    }

    // ─── Fixtures ──────────────────────────────────────────────────────────────

    private Subscription buildSubscription(Long id, SubscriptionPlan plan, SubscriptionStatus status) {
        return Subscription.builder()
                .subscriptionId(id)
                .recruiterId(1L)
                .plan(plan)
                .startDate(LocalDate.now())
                .endDate(plan == SubscriptionPlan.FREE ? null : LocalDate.now().plusDays(30))
                .status(status)
                .amountPaid(planPrice(plan))
                .maxJobPosts(planMaxPosts(plan))
                .autoRenew(false)
                .build();
    }

    private SubscriptionResponse buildSubResponse(Subscription s) {
        return SubscriptionResponse.builder()
                .subscriptionId(s.getSubscriptionId())
                .recruiterId(s.getRecruiterId())
                .plan(s.getPlan())
                .status(s.getStatus())
                .amountPaid(s.getAmountPaid())
                .maxJobPosts(s.getMaxJobPosts())
                .isActive(s.isActive())
                .build();
    }

    private InvoiceResponse buildInvoiceResponse(Long subId) {
        return InvoiceResponse.builder()
                .invoiceId(1L)
                .subscriptionId(subId)
                .recruiterId(1L)
                .amount(1999.0)
                .gstAmount(359.82)
                .totalAmount(2358.82)
                .paymentMode(PaymentMode.UPI)
                .paymentDate(LocalDateTime.now())
                .invoiceNumber("HC-INV-20260101-ABCD1234")
                .planName("PROFESSIONAL")
                .build();
    }

    private double planPrice(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> 0.0;
            case PROFESSIONAL -> 1999.0;
            case ENTERPRISE -> 4999.0;
        };
    }

    private int planMaxPosts(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> 3;
            case PROFESSIONAL -> 50;
            case ENTERPRISE -> 999;
        };
    }

    // ─── subscribe() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("subscribe()")
    class SubscribeTests {

        @Test
        @DisplayName("should create PROFESSIONAL subscription and generate invoice")
        void shouldCreateProfessionalSubscription() {
            SubscribeRequest request = new SubscribeRequest();
            request.setPlan(SubscriptionPlan.PROFESSIONAL);
            request.setPaymentMode(PaymentMode.UPI);

            Subscription saved = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);
            SubscriptionResponse response = buildSubResponse(saved);

            when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(saved);
            when(invoiceRepository.save(any(Invoice.class))).thenReturn(Invoice.builder().build());
            when(subscriptionMapper.toResponse(saved)).thenReturn(response);

            SubscriptionResponse result = subscriptionService.subscribe(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getPlan()).isEqualTo(SubscriptionPlan.PROFESSIONAL);
            assertThat(result.getMaxJobPosts()).isEqualTo(50);
            assertThat(result.getAmountPaid()).isEqualTo(1999.0);

            // Invoice must be generated for paid plan
            verify(invoiceRepository).save(any(Invoice.class));
        }

        @Test
        @DisplayName("should create FREE subscription without generating invoice")
        void shouldCreateFreeSubscriptionWithoutInvoice() {
            SubscribeRequest request = new SubscribeRequest();
            request.setPlan(SubscriptionPlan.FREE);
            request.setPaymentMode(PaymentMode.WALLET);

            Subscription saved = buildSubscription(1L, SubscriptionPlan.FREE, SubscriptionStatus.ACTIVE);
            SubscriptionResponse response = buildSubResponse(saved);

            when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(saved);
            when(subscriptionMapper.toResponse(saved)).thenReturn(response);

            SubscriptionResponse result = subscriptionService.subscribe(1L, request);

            assertThat(result.getAmountPaid()).isEqualTo(0.0);
            // No invoice for free plan
            verify(invoiceRepository, never()).save(any(Invoice.class));
        }

        @Test
        @DisplayName("should cancel existing subscription before creating new one")
        void shouldCancelExistingBeforeNewSubscription() {
            SubscribeRequest request = new SubscribeRequest();
            request.setPlan(SubscriptionPlan.ENTERPRISE);
            request.setPaymentMode(PaymentMode.CREDIT_CARD);

            Subscription existing = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);
            Subscription newSub   = buildSubscription(2L, SubscriptionPlan.ENTERPRISE, SubscriptionStatus.ACTIVE);
            SubscriptionResponse response = buildSubResponse(newSub);

            when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.of(existing));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(newSub);
            when(invoiceRepository.save(any(Invoice.class))).thenReturn(Invoice.builder().build());
            when(subscriptionMapper.toResponse(newSub)).thenReturn(response);

            subscriptionService.subscribe(1L, request);

            // existing must be cancelled — save called at least twice (cancel + new)
            verify(subscriptionRepository, atLeast(2)).save(any(Subscription.class));
        }
    }

    // ─── cancelSubscription() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelSubscription()")
    class CancelTests {

        @Test
        @DisplayName("should cancel active subscription")
        void shouldCancelActiveSubscription() {
            Subscription active = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);
            SubscriptionResponse response = buildSubResponse(active);
            response.setStatus(SubscriptionStatus.CANCELLED);

            when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.of(active));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(active);
            when(subscriptionMapper.toResponse(any())).thenReturn(response);

            SubscriptionResponse result = subscriptionService.cancelSubscription(1L);

            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
            verify(subscriptionRepository).save(argThat(s -> s.getStatus() == SubscriptionStatus.CANCELLED));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when no active subscription")
        void shouldThrowWhenNoActiveSubscription() {
            when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.cancelSubscription(1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No active subscription");
        }
    }

    // ─── getActiveSubscription() ───────────────────────────────────────────────

    @Nested
    @DisplayName("getActiveSubscription()")
    class GetActiveTests {

        @Test
        @DisplayName("should return active subscription when it exists")
        void shouldReturnActiveSubscription() {
            Subscription active = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);
            SubscriptionResponse response = buildSubResponse(active);

            when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.of(active));
            when(subscriptionMapper.toResponse(active)).thenReturn(response);

            SubscriptionResponse result = subscriptionService.getActiveSubscription(1L);

            assertThat(result.getPlan()).isEqualTo(SubscriptionPlan.PROFESSIONAL);
        }

        @Test
        @DisplayName("should return FREE plan when no active subscription exists")
        void shouldReturnFreePlanWhenNoActiveSubscription() {
            when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
                    .thenReturn(Optional.empty());
            when(subscriptionMapper.toResponse(any(Subscription.class))).thenAnswer(inv -> {
                Subscription s = inv.getArgument(0);
                return buildSubResponse(s);
            });

            SubscriptionResponse result = subscriptionService.getActiveSubscription(1L);

            assertThat(result.getPlan()).isEqualTo(SubscriptionPlan.FREE);
        }
    }

    // ─── hasActiveSubscription() ───────────────────────────────────────────────

    @Test
    @DisplayName("hasActiveSubscription() should return true when active subscription exists")
    void shouldReturnTrueWhenActiveSubscriptionExists() {
        Subscription active = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(active));

        assertThat(subscriptionService.hasActiveSubscription(1L)).isTrue();
    }

    @Test
    @DisplayName("hasActiveSubscription() should return false when no subscription")
    void shouldReturnFalseWhenNoActiveSubscription() {
        when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        assertThat(subscriptionService.hasActiveSubscription(1L)).isFalse();
    }

    // ─── getMaxJobPosts() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getMaxJobPosts() should return 50 for PROFESSIONAL plan")
    void shouldReturn50ForProfessionalPlan() {
        Subscription sub = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(sub));

        assertThat(subscriptionService.getMaxJobPosts(1L)).isEqualTo(50);
    }

    @Test
    @DisplayName("getMaxJobPosts() should return 3 (FREE) when no subscription exists")
    void shouldReturn3WhenNoSubscription() {
        when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        assertThat(subscriptionService.getMaxJobPosts(1L)).isEqualTo(3);
    }

    // ─── getInvoiceById() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getInvoiceById() should return invoice response")
    void shouldReturnInvoiceById() {
        Invoice invoice = Invoice.builder()
                .invoiceId(1L)
                .subscriptionId(1L)
                .recruiterId(1L)
                .amount(1999.0)
                .gstAmount(359.82)
                .totalAmount(2358.82)
                .paymentMode(PaymentMode.UPI)
                .paymentDate(LocalDateTime.now())
                .invoiceNumber("HC-INV-20260101-ABCD1234")
                .planName("PROFESSIONAL")
                .build();

        InvoiceResponse response = buildInvoiceResponse(1L);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(subscriptionMapper.toInvoiceResponse(invoice)).thenReturn(response);

        InvoiceResponse result = subscriptionService.getInvoiceById(1L);

        assertThat(result.getInvoiceId()).isEqualTo(1L);
        assertThat(result.getPlanName()).isEqualTo("PROFESSIONAL");
    }

    @Test
    @DisplayName("getInvoiceById() should throw ResourceNotFoundException when not found")
    void shouldThrowWhenInvoiceNotFound() {
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getInvoiceById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("renewSubscription() should delegate to subscribe")
    void shouldRenewSubscription() {
        SubscribeRequest request = new SubscribeRequest();
        request.setPlan(SubscriptionPlan.FREE);
        Subscription saved = buildSubscription(1L, SubscriptionPlan.FREE, SubscriptionStatus.ACTIVE);
        SubscriptionResponse response = buildSubResponse(saved);

        when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class))).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(saved);
        when(subscriptionMapper.toResponse(saved)).thenReturn(response);

        assertThat(subscriptionService.renewSubscription(1L, request).getPlan()).isEqualTo(SubscriptionPlan.FREE);
    }

    @Test
    @DisplayName("getPlans() should return all plan definitions")
    void shouldReturnPlanDefinitions() {
        var plans = subscriptionService.getPlans();

        assertThat(plans).extracting("plan")
                .containsExactly(SubscriptionPlan.FREE, SubscriptionPlan.PROFESSIONAL, SubscriptionPlan.ENTERPRISE);
        assertThat(plans.get(1).getTotalAmount()).isEqualTo(2358.82);
    }

    @Test
    @DisplayName("generateInvoice() should return latest invoice for subscription")
    void shouldGenerateInvoiceResponse() {
        Subscription subscription = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);
        Invoice invoice = Invoice.builder().invoiceId(5L).subscriptionId(1L).recruiterId(1L).build();
        InvoiceResponse response = buildInvoiceResponse(1L);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        when(invoiceRepository.findFirstBySubscriptionIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(invoice));
        when(subscriptionMapper.toInvoiceResponse(invoice)).thenReturn(response);

        assertThat(subscriptionService.generateInvoice(1L).getInvoiceId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("invoice queries should map repository results")
    void shouldReturnInvoiceLists() {
        Invoice invoice = Invoice.builder().invoiceId(1L).subscriptionId(1L).recruiterId(1L).build();
        InvoiceResponse response = buildInvoiceResponse(1L);

        when(invoiceRepository.findByRecruiterIdOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(invoice)));
        when(invoiceRepository.findByRecruiterIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(invoice));
        when(subscriptionMapper.toInvoiceResponse(invoice)).thenReturn(response);

        assertThat(subscriptionService.getInvoicesByRecruiter(1L, PageRequest.of(0, 10))).hasSize(1);
        assertThat(subscriptionService.getAllInvoicesByRecruiter(1L)).hasSize(1);
    }

    @Test
    @DisplayName("getAllSubscriptionsByRecruiter() should map subscriptions")
    void shouldReturnAllSubscriptionsByRecruiter() {
        Subscription subscription = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findByRecruiterId(1L)).thenReturn(List.of(subscription));
        when(subscriptionMapper.toResponse(subscription)).thenReturn(buildSubResponse(subscription));

        assertThat(subscriptionService.getAllSubscriptionsByRecruiter(1L)).hasSize(1);
    }

    @Test
    @DisplayName("createRazorpayOrder() should create paid order")
    void shouldCreateRazorpayOrder() {
        RazorpayOrderRequest request = new RazorpayOrderRequest();
        request.setPlan(SubscriptionPlan.PROFESSIONAL);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", "order_123")));

        RazorpayOrderResponse response = subscriptionService.createRazorpayOrder(1L, request);

        assertThat(response.getOrderId()).isEqualTo("order_123");
        assertThat(response.getAmount()).isEqualTo(235882);
    }

    @Test
    @DisplayName("verifyRazorpayPayment() should reject invalid signatures")
    void shouldRejectInvalidRazorpaySignature() {
        RazorpayVerifyRequest request = new RazorpayVerifyRequest();
        request.setPlan(SubscriptionPlan.PROFESSIONAL);
        request.setRazorpayOrderId("order_123");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("bad-signature");

        assertThatThrownBy(() -> subscriptionService.verifyRazorpayPayment(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment verification failed");
    }
}
