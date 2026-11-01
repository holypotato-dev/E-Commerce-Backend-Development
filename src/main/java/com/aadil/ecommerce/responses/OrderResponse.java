package com.aadil.ecommerce.responses;

import com.aadil.ecommerce.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;

    private OrderStatus orderStatus;

    private BigDecimal totalAmount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<OrderItemResponseDTO> orderedItems;
}
