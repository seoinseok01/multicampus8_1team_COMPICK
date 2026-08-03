package com.boot.compick.order.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.boot.compick.order.entity.OrderItemEntity;
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
	List<OrderItemEntity> findAllByOrderIdOrderById(Long orderId);
}
