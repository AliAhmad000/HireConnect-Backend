package com.hireconnect.subscription.dto.request;

import com.hireconnect.subscription.enums.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RazorpayOrderRequest {

    @NotNull(message = "Plan is required")
    @Schema(example = "PROFESSIONAL")
    private SubscriptionPlan plan;
}
