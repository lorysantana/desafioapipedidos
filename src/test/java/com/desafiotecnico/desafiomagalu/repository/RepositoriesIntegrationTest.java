package com.desafiotecnico.desafiomagalu.repository;

import com.desafiotecnico.desafiomagalu.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RepositoriesIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private UserEntity createUser(Long userId, String name) {
        return UserEntity.builder()
                .userId(userId)
                .name(name)
                .build();
    }

    private ProductEntity createProduct(Long productId) {
        return ProductEntity.builder()
                .productId(productId)
                .build();
    }

    private OrderEntity createOrder(Long orderId, UserEntity user, LocalDate date, BigDecimal total) {
        OrderEntity order = OrderEntity.builder()
                .orderId(orderId)
                .user(user)
                .date(date)
                .total(total)
                .build();
        user.getOrders().add(order);
        return order;
    }

    private void createOrderItem(Long orderId, Long productId, OrderEntity order, ProductEntity product, BigDecimal value) {
        OrderItemId id = new OrderItemId(orderId, productId);
        OrderItemEntity item = OrderItemEntity.builder()
                .id(id)
                .order(order)
                .product(product)
                .value(value)
                .build();
        order.getItems().add(item);
    }

    @Test
    @DisplayName("should persist user -> order -> order_item -> product and retrieve them via repositories")
    void persistUserOrderItemProduct_andFind() {
        UserEntity u1 = createUser(1L, "Zarelli");
        ProductEntity p111 = createProduct(111L);
        ProductEntity p122 = createProduct(122L);

        productRepository.saveAll(List.of(p111, p122));

        OrderEntity o123 = createOrder(123L, u1, LocalDate.of(2021, 12, 1), new BigDecimal("1024.48"));
        createOrderItem(123L, 111L, o123, p111, new BigDecimal("512.24"));
        createOrderItem(123L, 122L, o123, p122, new BigDecimal("512.24"));

        userRepository.save(u1);

        Optional<UserEntity> foundUserOpt = userRepository.findByUserId(1L);
        assertThat(foundUserOpt).isPresent();
        UserEntity foundUser = foundUserOpt.get();
        assertThat(foundUser.getName()).isEqualTo("Zarelli");
        assertThat(foundUser.getOrders()).hasSize(1);

        OrderEntity foundOrder = foundUser.getOrders().getFirst();
        assertThat(foundOrder.getOrderId()).isEqualTo(123L);
        assertThat(foundOrder.getItems()).hasSize(2);

        List<OrderItemEntity> items = foundOrder.getItems();
        assertThat(items).extracting("value")
                .containsExactlyInAnyOrder(new BigDecimal("512.24"), new BigDecimal("512.24"));

        OrderItemId id111 = new OrderItemId(123L, 111L);
        Optional<OrderItemEntity> itemOpt = orderItemRepository.findById(id111);
        assertThat(itemOpt).isPresent();
        assertThat(itemOpt.get().getValue()).isEqualByComparingTo(new BigDecimal("512.24"));
    }

    @Test
    @DisplayName("should fetch items and products when querying orders by date range using EntityGraph")
    void findByDateBetween_entityGraph_fetches_items_and_product() {
        UserEntity u1 = createUser(1L, "Zarelli");
        ProductEntity p111 = createProduct(111L);
        productRepository.save(p111);

        OrderEntity o123 = createOrder(123L, u1, LocalDate.of(2021, 12, 1), new BigDecimal("512.24"));
        createOrderItem(123L, 111L, o123, p111, new BigDecimal("512.24"));

        userRepository.save(u1);

        LocalDate start = LocalDate.of(2021, 11, 30);
        LocalDate end = LocalDate.of(2021, 12, 2);
        List<OrderEntity> orders = orderRepository.findByDateBetween(start, end);

        assertThat(orders).isNotEmpty();
        OrderEntity order = orders.stream().filter(o -> o.getOrderId().equals(123L)).findFirst().orElseThrow();
        assertThat(order.getItems()).hasSize(1);
        OrderItemEntity item = order.getItems().getFirst();
        assertThat(item.getProduct()).isNotNull();
        assertThat(item.getProduct().getProductId()).isEqualTo(111L);
        assertThat(item.getValue()).isEqualByComparingTo(new BigDecimal("512.24"));
    }

    @Test
    @DisplayName("should remove associated order_items when an order is deleted")
    void deleteOrder_removes_orderItems_due_to_orphanRemoval() {
        UserEntity u1 = createUser(10L, "DeleteTestUser");
        ProductEntity p1 = createProduct(200L);
        productRepository.save(p1);

        OrderEntity o1 = createOrder(2000L, u1, LocalDate.now(), null);
        createOrderItem(2000L, 200L, o1, p1, new BigDecimal("100.00"));

        userRepository.save(u1);

        OrderItemId id = new OrderItemId(2000L, 200L);
        assertThat(orderItemRepository.findById(id)).isPresent();

        OrderEntity persistedOrder = orderRepository.findById(2000L).orElseThrow();
        orderRepository.delete(persistedOrder);
        assertThat(orderItemRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("should not duplicate product in catalog when importing multiple lines with same product_id")
    void importMultipleLines_doNotDuplicateProduct() {
        ProductEntity p = createProduct(300L);
        productRepository.save(p);

        UserEntity u1 = createUser(100L, "UserA");
        OrderEntity o1 = createOrder(1000L, u1, LocalDate.of(2020, 12, 1), null);
        createOrderItem(1000L, 300L, o1, p, new BigDecimal("256.24"));

        UserEntity u2 = createUser(200L, "UserB");
        OrderEntity o2 = createOrder(2000L, u2, LocalDate.of(2021, 12, 1), null);
        createOrderItem(2000L, 300L, o2, p, new BigDecimal("512.24"));

        userRepository.save(u1);
        userRepository.save(u2);

        List<ProductEntity> products = productRepository.findAll();
        assertThat(products).filteredOn(prod -> prod.getProductId().equals(300L)).hasSize(1);

        List<OrderItemEntity> items = orderItemRepository.findAll()
                .stream()
                .filter(i -> i.getProduct() != null && i.getProduct().getProductId().equals(300L))
                .collect(Collectors.toList());

        assertThat(items).hasSize(2);
    }
}
