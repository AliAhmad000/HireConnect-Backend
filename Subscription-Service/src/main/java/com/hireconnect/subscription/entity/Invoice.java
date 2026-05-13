package com.hireconnect.subscription.entity;

import com.hireconnect.subscription.enums.PaymentMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "invoices",
    indexes = {
        @Index(name = "idx_invoice_subscription", columnList = "subscription_id"),
        @Index(name = "idx_invoice_recruiter",     columnList = "recruiter_id")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceId;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "recruiter_id", nullable = false)
    private Long recruiterId;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 20)
    private PaymentMode paymentMode;

    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "invoice_number", unique = true)
    private String invoiceNumber;

    @Column(name = "plan_name")
    private String planName;

    @Column(name = "gst_amount")
    private Double gstAmount;

    @Column(name = "total_amount")
    private Double totalAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
