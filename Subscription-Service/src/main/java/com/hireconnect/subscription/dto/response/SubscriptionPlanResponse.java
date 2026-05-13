package com.hireconnect.subscription.dto.response;

import com.hireconnect.subscription.enums.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SubscriptionPlanResponse {
    private SubscriptionPlan plan;
    private String name;
    private Double price;
    private Double gstAmount;
    private Double totalAmount;
    private Integer maxJobPosts;
    private Integer durationDays;
    private List<String> features;
}
