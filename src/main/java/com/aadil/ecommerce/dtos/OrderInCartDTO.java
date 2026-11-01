package com.aadil.ecommerce.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderInCartDTO {

    @NotNull(message = "Order items list cannot be null")
    @NotEmpty(message = "Cart must contain at least one item")
    @Size(min = 1, max = 10, message = "Ordered items count must be between 1 and 10")
    @Valid
    private List<OrderRequestDTO> orderItems = new ArrayList<>();
}
