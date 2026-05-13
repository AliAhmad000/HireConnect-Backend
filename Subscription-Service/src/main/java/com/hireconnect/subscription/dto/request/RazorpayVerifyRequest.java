package com.hireconnect.subscription.dto.request;

import com.hireconnect.subscription.enums.PaymentMode;
import com.hireconnect.subscription.enums.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RazorpayVerifyRequest {

    @NotNull(message = "Plan is required")
    @Schema(example = "PROFESSIONAL")
    private SubscriptionPlan plan;

    @NotNull(message = "Payment mode is required")
    @Schema(example = "UPI")
    private PaymentMode paymentMode;

    @Schema(example = "false")
    private Boolean autoRenew = false;

    @NotBlank(message = "Razorpay order id is required")
    @Schema(example = "order_Nw0abc123")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment id is required")
    @Schema(example = "pay_Nw0xyz456")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    @Schema(example = "7f4f3f8d9a5b6c1e2d3a4b5c6d7e8f90")
    private String razorpaySignature;
}
