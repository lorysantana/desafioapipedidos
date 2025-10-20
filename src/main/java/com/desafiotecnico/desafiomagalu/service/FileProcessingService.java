package com.desafiotecnico.desafiomagalu.service;

import com.desafiotecnico.desafiomagalu.dto.UserResponseDto;
import com.desafiotecnico.desafiomagalu.dto.UserResponseDto.OrderDto;
import com.desafiotecnico.desafiomagalu.dto.UserResponseDto.ProductDto;
import com.desafiotecnico.desafiomagalu.exception.BadFileFormatException;
import com.desafiotecnico.desafiomagalu.model.*;
import com.desafiotecnico.desafiomagalu.parser.LegacyLineParser;
import com.desafiotecnico.desafiomagalu.parser.LegacyLineParser.ParsedLine;
import com.desafiotecnico.desafiomagalu.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileProcessingService {

    private final LegacyLineParser parser;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public List<UserResponseDto> processFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadFileFormatException("Arquivo nulo ou vazio");
        }

        Map<Long, UserAccumulator> users = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                ParsedLine parsed = parser.parse(line);

                final Long userId = parsed.getUserId();
                final String userName = parsed.getUserName();
                final Long orderId = parsed.getOrderId();
                final LocalDate orderDate = parsed.getDate();
                final Long productId = parsed.getProductId();
                final BigDecimal value = parsed.getValue();

                UserAccumulator ua = users.computeIfAbsent(userId, id -> new UserAccumulator(userName));
                OrderAccumulator oa = ua.orders.computeIfAbsent(orderId, id -> new OrderAccumulator(orderDate));

                oa.products.add(new ProductLine(productId, value));
                oa.total = oa.total.add(value);
            }
        } catch (BadFileFormatException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao processar arquivo", e);
            throw new RuntimeException("Erro ao processar arquivo: " + e.getMessage(), e);
        }

        List<UserResponseDto> result = new ArrayList<>();

        for (Map.Entry<Long, UserAccumulator> ue : users.entrySet()) {
            Long userId = ue.getKey();
            UserAccumulator ua = ue.getValue();

            UserEntity userEntity = userRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        UserEntity u = new UserEntity();
                        u.setUserId(userId);
                        u.setName(ua.name);
                        return userRepository.save(u);
                    });
            userEntity.setName(ua.name);
            userEntity = userRepository.save(userEntity);

            final UserEntity userFinal = userEntity;

            UserResponseDto userDto = new UserResponseDto();
            userDto.setUserId(userId);
            userDto.setName(ua.name);
            userDto.setOrders(new ArrayList<>());

            for (Map.Entry<Long, OrderAccumulator> oe : ua.orders.entrySet()) {
                Long orderId = oe.getKey();
                OrderAccumulator oa = oe.getValue();

                OrderEntity orderEntity = orderRepository.findByOrderId(orderId)
                        .orElseGet(() -> {
                            OrderEntity o = new OrderEntity();
                            o.setOrderId(orderId);
                            o.setUser(userFinal);
                            return o;
                        });
                orderEntity.setUser(userFinal);
                orderEntity.setDate(oa.date);
                orderEntity.setTotal(oa.total);
                orderEntity = orderRepository.save(orderEntity);

                final OrderEntity orderFinal = orderEntity;

                List<ProductDto> productsDto = new ArrayList<>();
                for (ProductLine pl : oa.products) {
                    ProductEntity productEntity = productRepository.findById(pl.productId)
                            .orElseGet(() -> {
                                ProductEntity p = new ProductEntity();
                                p.setProductId(pl.productId);
                                return productRepository.save(p);
                            });

                    final ProductEntity productFinal = productEntity;
                    final BigDecimal valFinal = pl.value;

                    OrderItemId itemId = new OrderItemId(orderFinal.getOrderId(), productFinal.getProductId());
                    OrderItemEntity itemEntity = orderItemRepository.findById(itemId)
                            .orElseGet(() -> {
                                OrderItemEntity it = new OrderItemEntity();
                                it.setId(itemId);
                                it.setOrder(orderFinal);
                                it.setProduct(productFinal);
                                it.setValue(valFinal);
                                return it;
                            });

                    itemEntity.setOrder(orderFinal);
                    itemEntity.setProduct(productFinal);
                    itemEntity.setValue(valFinal);
                    orderItemRepository.save(itemEntity);

                    ProductDto pDto = new ProductDto();
                    pDto.setProductId(productFinal.getProductId());
                    pDto.setValue(format(valFinal));
                    productsDto.add(pDto);
                }

                OrderDto orderDto = new OrderDto();
                orderDto.setOrderId(orderId);
                orderDto.setDate(oa.date.toString());
                orderDto.setTotal(format(oa.total));
                orderDto.setProducts(productsDto);

                userDto.getOrders().add(orderDto);
            }

            result.add(userDto);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> query(Optional<Long> optOrderId,
                                       Optional<LocalDate> optStart,
                                       Optional<LocalDate> optEnd) {

        List<OrderEntity> orders;
        if (optOrderId.isPresent()) {
            orders = orderRepository.findByOrderId(optOrderId.get())
                    .map(List::of)
                    .orElseGet(Collections::emptyList);
        } else if (optStart.isPresent() && optEnd.isPresent()) {
            orders = orderRepository.findByDateBetween(optStart.get(), optEnd.get());
        } else {
            orders = orderRepository.findAll();
        }

        Map<Long, UserResponseDto> users = new LinkedHashMap<>();

        for (OrderEntity oe : orders) {
            UserEntity ue = oe.getUser();
            if (ue == null) continue;

            UserResponseDto uDto = users.computeIfAbsent(ue.getUserId(), id -> {
                UserResponseDto u = new UserResponseDto();
                u.setUserId(ue.getUserId());
                u.setName(ue.getName());
                u.setOrders(new ArrayList<>());
                return u;
            });

            OrderDto ord = new OrderDto();
            ord.setOrderId(oe.getOrderId());
            ord.setDate(oe.getDate() == null ? null : oe.getDate().toString());
            ord.setTotal(format(oe.getTotal()));
            ord.setProducts(new ArrayList<>());

            if (oe.getItems() != null) {
                for (OrderItemEntity item : oe.getItems()) {
                    ProductEntity p = item.getProduct();
                    ProductDto pDto = new ProductDto();
                    pDto.setProductId(p.getProductId());
                    pDto.setValue(format(item.getValue()));
                    ord.getProducts().add(pDto);
                }
            }

            uDto.getOrders().add(ord);
        }

        return new ArrayList<>(users.values());
    }

    // ---------------- helpers ----------------

    private static class UserAccumulator {
        final String name;
        final Map<Long, OrderAccumulator> orders = new LinkedHashMap<>();
        UserAccumulator(String name) { this.name = name; }
    }

    private static class OrderAccumulator {
        final LocalDate date;
        final List<ProductLine> products = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        OrderAccumulator(LocalDate date) { this.date = date; }
    }

    private static record ProductLine(Long productId, BigDecimal value) {}

    private static String format(BigDecimal b) {
        if (b == null) return "0.00";
        return b.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
