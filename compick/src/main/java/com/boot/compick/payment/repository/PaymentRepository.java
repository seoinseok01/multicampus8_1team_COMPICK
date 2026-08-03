package com.boot.compick.payment.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.boot.compick.payment.entity.PaymentEntity;
import java.util.Optional;
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
	boolean existsByOrderId(Long orderId);
	Optional<PaymentEntity> findByOrderId(Long orderId);
}
