package com.hireconnect.subscription.dto.response;

import com.hireconnect.subscription.enums.PaymentMode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceResponse {
    private Long invoiceId;
    private Long subscriptionId;
    private Long recruiterId;
    private Double amount;
    private Double gstAmount;
    private Double totalAmount;
    private LocalDateTime paymentDate;
    private PaymentMode paymentMode;
    private String transactionId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String invoiceNumber;
    private String planName;
    private LocalDateTime createdAt;
}
