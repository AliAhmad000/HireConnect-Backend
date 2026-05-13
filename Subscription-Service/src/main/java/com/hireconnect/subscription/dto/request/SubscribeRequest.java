package com.hireconnect.subscription.dto.request;

import com.hireconnect.subscription.enums.PaymentMode;
import com.hireconnect.subscription.enums.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscribeRequest {

    @NotNull(message = "Plan is required")
    @Schema(example = "PROFESSIONAL")
    private SubscriptionPlan plan;

    @NotNull(message = "Payment mode is required")
    @Schema(example = "UPI")
    private PaymentMode paymentMode;

    @Schema(example = "false")
    private Boolean autoRenew = false;

    @Schema(example = "pay_test_token_123")
    private String paymentToken;

    @Schema(example = "order_Nw0abc123")
    private String razorpayOrderId;

    @Schema(example = "pay_Nw0xyz456")
    private String razorpayPaymentId;

    @Schema(example = "7f4f3f8d9a5b6c1e2d3a4b5c6d7e8f90")
    private String razorpaySignature;
}
