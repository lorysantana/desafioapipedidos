package com.desafiotecnico.desafiomagalu.repository;

import com.desafiotecnico.desafiomagalu.model.OrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByOrderId(Long orderId);
    @EntityGraph(attributePaths = {"items", "items.product"})
    List<OrderEntity> findByUserUserId(Long userId);
    @org.springframework.data.jpa.repository.Query(
            "select o from OrderEntity o " +
                    "left join fetch o.items i " +
                    "left join fetch i.product p " +
                    "where o.orderId = :orderId"
    )
    Optional<OrderEntity> findByOrderIdWithItems(Long orderId);
    List<OrderEntity> findByDateBetween(LocalDate start, LocalDate end);
}
