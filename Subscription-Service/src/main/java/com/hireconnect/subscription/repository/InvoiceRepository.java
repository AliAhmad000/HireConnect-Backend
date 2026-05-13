package com.hireconnect.subscription.repository;

import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.enums.PaymentMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findBySubscriptionId(Long subscriptionId);

    Page<Invoice> findByRecruiterIdOrderByCreatedAtDesc(Long recruiterId, Pageable pageable);

    List<Invoice> findByRecruiterIdOrderByCreatedAtDesc(Long recruiterId);

    List<Invoice> findByPaymentMode(PaymentMode paymentMode);

    Optional<Invoice> findFirstBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);

    Optional<Invoice> findByTransactionId(String transactionId);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}
