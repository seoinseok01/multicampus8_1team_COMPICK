package com.boot.compick.order.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.boot.compick.order.entity.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
	Optional<OrderEntity> findByOrderNumberAndMemberId(String orderNumber, Long memberId);
}
