package com.aadil.ecommerce.responses;

import com.aadil.ecommerce.entities.OrderEntity;
import com.aadil.ecommerce.entities.OrderItemEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
public class OrderResponseMapper {

    public OrderItemResponseDTO mapToOrderItemResponseDTO(OrderItemEntity orderItem) {
        BigDecimal unitPrice = orderItem.getUnitPrice();
        Integer quantity = orderItem.getQuantity();
        BigDecimal subtotal = (unitPrice != null && quantity != null)
                ? unitPrice.multiply(BigDecimal.valueOf(quantity))
                : BigDecimal.ZERO;

        String productName = (orderItem.getProduct() != null)
                ? orderItem.getProduct().getProductName()
                : null;

        Long productId = (orderItem.getProduct() != null)
                ? orderItem.getProduct().getProductId()
                : null;

        return OrderItemResponseDTO.builder()
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .build();
    }

    public OrderResponse mapToResponse(OrderEntity order) {
        List<OrderItemResponseDTO> orderedItems = (order.getOrderItems() != null)
                ? order.getOrderItems().stream().map(this::mapToOrderItemResponseDTO).toList()
                : Collections.emptyList();

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderStatus(order.getOrderStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .orderedItems(orderedItems)
                .build();
    }
}
