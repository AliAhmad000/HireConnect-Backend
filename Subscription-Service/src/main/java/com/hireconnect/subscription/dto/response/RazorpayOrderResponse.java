package com.hireconnect.subscription.dto.response;

import com.hireconnect.subscription.enums.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RazorpayOrderResponse {
    private String keyId;
    private String orderId;
    private String currency;
    private Integer amount;
    private Double amountRupees;
    private Double gstAmount;
    private Double totalAmount;
    private SubscriptionPlan plan;
    private String receipt;
}
