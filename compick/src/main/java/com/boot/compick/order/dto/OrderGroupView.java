package com.boot.compick.order.dto;

import java.util.List;
import com.boot.compick.order.entity.OrderItemEntity;

public record OrderGroupView(
        Long id,
        String name,
        String type,
        int quantity,
        List<OrderItemEntity> items,
        long amount) {
}
