package com.desafiotecnico.desafiomagalu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import java.util.List;

@Data
@JsonPropertyOrder({"user_id", "name", "orders"})
public class UserResponseDto {

    @JsonProperty("user_id")
    private Long userId;

    private String name;

    private List<OrderDto> orders;

    @Data
    @JsonPropertyOrder({"order_id", "total", "date", "products"})
    public static class OrderDto {
        @JsonProperty("order_id")
        private Long orderId;

        private String total;
        private String date;

        private List<ProductDto> products;
    }

    @Data
    @JsonPropertyOrder({"product_id", "value"})
    public static class ProductDto {
        @JsonProperty("product_id")
        private Long productId;

        private String value;
    }
}