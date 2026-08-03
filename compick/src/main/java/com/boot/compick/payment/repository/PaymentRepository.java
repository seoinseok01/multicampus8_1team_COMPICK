package com.boot.compick.payment.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.boot.compick.payment.entity.PaymentEntity;
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
	boolean existsByOrderId(Long orderId);
}
