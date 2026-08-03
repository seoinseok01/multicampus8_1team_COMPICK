package com.boot.compick.order.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.boot.compick.order.entity.OrderGroupEntity;
public interface OrderGroupRepository extends JpaRepository<OrderGroupEntity, Long> {}
