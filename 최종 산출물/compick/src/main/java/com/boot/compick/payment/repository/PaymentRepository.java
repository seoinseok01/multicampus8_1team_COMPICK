package com.boot.compick.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.compick.payment.entity.PaymentEntity;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

	Optional<PaymentEntity> findByOrderId(Long orderId);

	Optional<PaymentEntity> findByExternalTransactionId(String externalTransactionId);
}
