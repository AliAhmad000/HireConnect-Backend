package com.hireconnect.subscription.mapper;

import com.hireconnect.subscription.dto.response.InvoiceResponse;
import com.hireconnect.subscription.dto.response.SubscriptionResponse;
import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    @Mapping(target = "isActive", expression = "java(subscription.isActive())")
    SubscriptionResponse toResponse(Subscription subscription);

    InvoiceResponse toInvoiceResponse(Invoice invoice);
}
