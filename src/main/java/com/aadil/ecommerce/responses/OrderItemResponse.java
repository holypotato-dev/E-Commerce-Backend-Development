package com.aadil.ecommerce.responses;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private Integer itemPurchased;
    private BigDecimal price;
    private BigDecimal totalAmount;
    
}
