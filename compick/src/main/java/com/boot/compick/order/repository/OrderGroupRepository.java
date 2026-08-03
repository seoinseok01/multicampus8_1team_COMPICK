package com.boot.compick.order.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.boot.compick.order.entity.OrderGroupEntity;
import java.util.List;
public interface OrderGroupRepository extends JpaRepository<OrderGroupEntity, Long> {
	List<OrderGroupEntity> findAllByOrderIdOrderById(Long orderId);
	void deleteAllByOrderId(Long orderId);
}
