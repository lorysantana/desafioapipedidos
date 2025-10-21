package com.desafiotecnico.desafiomagalu.service;

import com.desafiotecnico.desafiomagalu.dto.UserResponseDto;
import com.desafiotecnico.desafiomagalu.model.OrderItemEntity;
import com.desafiotecnico.desafiomagalu.model.OrderItemId;
import com.desafiotecnico.desafiomagalu.repository.OrderItemRepository;
import com.desafiotecnico.desafiomagalu.repository.OrderRepository;
import com.desafiotecnico.desafiomagalu.repository.ProductRepository;
import com.desafiotecnico.desafiomagalu.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FileProcessingServiceIntegrationTest {

    @Autowired private FileProcessingService fileProcessingService;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderItemRepository orderItemRepository;

    private static String buildLine(long userId,
                                    String userName,
                                    long orderId,
                                    long productId,
                                    String valueAsDecimal,
                                    String dateYmd) {
        String userIdRaw = String.format("%010d", userId);
        String userNameFixed = (userName == null ? "" : userName);
        if (userNameFixed.length() > 45) userNameFixed = userNameFixed.substring(0, 45);
        String userNameRaw = String.format("%-45s", userNameFixed);
        String orderIdRaw = String.format("%010d", orderId);
        String productIdRaw = String.format("%010d", productId);
        String valueRaw = String.format("%12s", valueAsDecimal).replace(' ', '0');
        return userIdRaw + userNameRaw + orderIdRaw + productIdRaw + valueRaw + dateYmd;
    }

    @Test
    @Transactional
    @DisplayName("should process legacy file end-to-end, persisting and returning expected structure")
    void processFile_endToEnd_should_persist_and_return_expected_structure() throws Exception {
        String l1 = buildLine(2L, "Medeiros", 12345L, 111L, "000000256.24", "20201201");
        String l2 = buildLine(1L, "Zarelli", 123L, 111L, "000000512.24", "20211201");
        String l3 = buildLine(1L, "Zarelli", 123L, 122L, "000000512.24", "20211201");
        String l4 = buildLine(2L, "Medeiros", 12345L, 122L, "000000256.24", "20201201");

        String all = String.join("\n", l1, l2, l3, l4) + "\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "legacy.txt", "text/plain",
                new ByteArrayInputStream(all.getBytes())
        );

        List<UserResponseDto> result = fileProcessingService.processFile(file);

        assertThat(result).hasSize(2);

        UserResponseDto z = result.stream()
                .filter(u -> u.getUserId().equals(1L))
                .findFirst()
                .orElse(null);
        assertThat(z).isNotNull();
        assertThat(z.getName()).isEqualTo("Zarelli");
        assertThat(z.getOrders()).hasSize(1);
        assertThat(z.getOrders().getFirst().getProducts()).hasSize(2);
        assertThat(z.getOrders().getFirst().getTotal()).isEqualTo("1024.48");

        UserResponseDto m = result.stream()
                .filter(u -> u.getUserId().equals(2L))
                .findFirst()
                .orElse(null);
        assertThat(m).isNotNull();
        assertThat(m.getOrders()).hasSize(1);
        assertThat(m.getOrders().getFirst().getProducts()).hasSize(2);
        assertThat(m.getOrders().getFirst().getTotal()).isEqualTo("512.48");

        assertThat(userRepository.findByUserId(1L)).isPresent();
        assertThat(userRepository.findByUserId(2L)).isPresent();
        assertThat(orderRepository.findByOrderId(123L)).isPresent();
        assertThat(orderRepository.findByOrderId(12345L)).isPresent();

        assertThat(productRepository.findById(111L)).isPresent();
        assertThat(productRepository.findById(122L)).isPresent();

        OrderItemId id111_123 = new OrderItemId(123L, 111L);
        OrderItemEntity item = orderItemRepository.findById(id111_123).orElseThrow();
        assertThat(item.getValue()).isEqualByComparingTo(new BigDecimal("512.24"));
    }
}