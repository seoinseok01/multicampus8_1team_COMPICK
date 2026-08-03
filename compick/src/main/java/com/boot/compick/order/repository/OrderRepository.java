package com.boot.compick.order.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.boot.compick.order.entity.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
	Optional<OrderEntity> findByOrderNumberAndMemberId(String orderNumber, Long memberId);
	Optional<OrderEntity> findTopByOrderNumberStartingWithOrderByOrderNumberDesc(String prefix);
	List<OrderEntity> findAllByMemberIdOrderByOrderedAtDesc(Long memberId);
}
