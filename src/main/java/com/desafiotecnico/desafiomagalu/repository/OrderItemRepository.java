package com.desafiotecnico.desafiomagalu.repository;

import com.desafiotecnico.desafiomagalu.model.OrderItemEntity;
import com.desafiotecnico.desafiomagalu.model.OrderItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, OrderItemId> {
    List<OrderItemEntity> findByOrderOrderId(Long orderId);
    List<OrderItemEntity> findByProductProductId(Long productId);
}
