package com.desafiotecnico.desafiomagalu.service;

import com.desafiotecnico.desafiomagalu.dto.UserResponseDto;
import com.desafiotecnico.desafiomagalu.model.*;
import com.desafiotecnico.desafiomagalu.parser.LegacyLineParser;
import com.desafiotecnico.desafiomagalu.parser.LegacyLineParser.ParsedLine;
import com.desafiotecnico.desafiomagalu.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class FileProcessingServiceUnitTest {

    @Mock private LegacyLineParser parser;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderItemRepository orderItemRepository;

    @InjectMocks
    private FileProcessingService fileProcessingService;

    @BeforeEach
    void setup() {
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.save(any(OrderItemEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("should parse file, persist entities and return expected DTO")
    void processFile_should_parse_and_persist_and_return_expected_dto() throws Exception {
        String fileContent = "L1\nL2\nL3\nL4\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain",
                new ByteArrayInputStream(fileContent.getBytes()));

        ParsedLine p1 = new ParsedLine(2L, "Medeiros", 12345L, 111L, new BigDecimal("256.24"), LocalDate.of(2020,12,1));
        ParsedLine p2 = new ParsedLine(1L, "Zarelli" , 123L , 111L, new BigDecimal("512.24"), LocalDate.of(2021,12,1));
        ParsedLine p3 = new ParsedLine(1L, "Zarelli" , 123L , 122L, new BigDecimal("512.24"), LocalDate.of(2021,12,1));
        ParsedLine p4 = new ParsedLine(2L, "Medeiros", 12345L, 122L, new BigDecimal("256.24"), LocalDate.of(2020,12,1));

        when(parser.parse(anyString()))
                .thenReturn(p1)
                .thenReturn(p2)
                .thenReturn(p3)
                .thenReturn(p4);

        when(userRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        when(orderRepository.findByOrderId(anyLong())).thenReturn(Optional.empty());
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(orderItemRepository.findById(any())).thenReturn(Optional.empty());

        List<UserResponseDto> result = fileProcessingService.processFile(file);

        assertThat(result).hasSize(2);

        UserResponseDto user1 = result.stream().filter(u -> u.getUserId().equals(1L)).findFirst().orElse(null);
        assertThat(user1).isNotNull();
        assertThat(user1.getName()).isEqualTo("Zarelli");
        assertThat(user1.getOrders()).hasSize(1);
        assertThat(user1.getOrders().getFirst().getProducts()).hasSize(2);
        assertThat(user1.getOrders().getFirst().getTotal()).isEqualTo("1024.48");

        UserResponseDto user2 = result.stream().filter(u -> u.getUserId().equals(2L)).findFirst().orElse(null);
        assertThat(user2).isNotNull();
        assertThat(user2.getOrders()).hasSize(1);
        assertThat(user2.getOrders().getFirst().getProducts()).hasSize(2);
        assertThat(user2.getOrders().getFirst().getTotal()).isEqualTo("512.48");

        verify(userRepository, atLeastOnce()).save(any(UserEntity.class));
        verify(orderRepository, atLeastOnce()).save(any(OrderEntity.class));
        verify(productRepository, atLeastOnce()).save(any(ProductEntity.class));
        verify(orderItemRepository, atLeastOnce()).save(any(OrderItemEntity.class));
    }
}
