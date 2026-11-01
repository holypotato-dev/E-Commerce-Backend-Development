package com.aadil.ecommerce.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {

    @NotNull(message = "Product ID cannot be null")
    @Min(value = 1, message = "Product ID must be greater than 0")
    private Long productId;

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10, message = "Maximum quantity per item is 10")
    private Integer quantity;
}
