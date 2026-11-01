package com.aadil.ecommerce.controllers;

import com.aadil.ecommerce.dtos.OrderInCartDTO;
import com.aadil.ecommerce.responses.APIResponse;
import com.aadil.ecommerce.responses.OrderResponse;
import com.aadil.ecommerce.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<APIResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderInCartDTO dto) {
        OrderResponse orderResponse = orderService.createOrder(dto);

        APIResponse<OrderResponse> apiResponse = APIResponse.<OrderResponse>builder()
                .success(true)
                .code(201)
                .data(orderResponse)
                .message("Order has successfully CREATED")
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }
}
