package com.aadil.ecommerce.controllers;

import com.aadil.ecommerce.dtos.OrderInCartDTO;
import com.aadil.ecommerce.enums.OrderStatus;
import com.aadil.ecommerce.enums.SortDirections;
import com.aadil.ecommerce.responses.APIResponse;
import com.aadil.ecommerce.responses.OrderResponse;
import com.aadil.ecommerce.responses.PaginatedResponse;
import com.aadil.ecommerce.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    // =========================================================================
    // USER ENDPOINTS (Accessible strictly by USER role)
    // =========================================================================

    /**
     * Place a new order with items from the customer's cart.
     * Accessible by: USER
     * Returns: 201 CREATED
     */
    @PostMapping({"", "/", "/create"})
    public ResponseEntity<APIResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderInCartDTO dto) {
        OrderResponse orderResponse = orderService.createOrder(dto);

        APIResponse<OrderResponse> apiResponse = APIResponse.<OrderResponse>builder()
                .success(true)
                .code(HttpStatus.CREATED.value())
                .message("Order created successfully")
                .data(orderResponse)
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    /**
     * Retrieve paginated order history for the authenticated customer.
     * Accessible by: USER
     * Returns: 200 OK
     */
    @GetMapping({"", "/", "/history"})
    public ResponseEntity<APIResponse<PaginatedResponse<List<OrderResponse>>>> getOrderHistory(
            @RequestParam(defaultValue = "0", name = "page", required = false) int page,
            @RequestParam(defaultValue = "10", name = "size", required = false) int size,
            @RequestParam(defaultValue = "DESC", name = "direction", required = false) SortDirections sortDirection) {

        PaginatedResponse<List<OrderResponse>> history = orderService.getOrderHistory(page, size, sortDirection);

        APIResponse<PaginatedResponse<List<OrderResponse>>> apiResponse = APIResponse.<PaginatedResponse<List<OrderResponse>>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Order history retrieved successfully")
                .data(history)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Retrieve single order details by ID for the owner.
     * Accessible by: USER
     * Returns: 200 OK
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<APIResponse<OrderResponse>> getOrderById(@PathVariable Long orderId) {
        OrderResponse orderResponse = orderService.getOrderById(orderId);

        APIResponse<OrderResponse> apiResponse = APIResponse.<OrderResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Order details retrieved successfully")
                .data(orderResponse)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Cancel an order if it is still in PENDING status and restore product stock.
     * Accessible by: USER
     * Returns: 200 OK
     */
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<APIResponse<OrderResponse>> cancelOrder(@PathVariable Long orderId) {
        OrderResponse orderResponse = orderService.cancelOrder(orderId);

        APIResponse<OrderResponse> apiResponse = APIResponse.<OrderResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Order cancelled successfully and stock restored")
                .data(orderResponse)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    // =========================================================================
    // ADMIN ENDPOINTS (Accessible strictly by ADMIN role)
    // =========================================================================

    /**
     * Admin: Retrieve all orders across all users with optional status filter.
     * Accessible by: ADMIN
     * Returns: 200 OK
     */
    @GetMapping("/admin/all")
    public ResponseEntity<APIResponse<PaginatedResponse<List<OrderResponse>>>> getAllOrdersForAdmin(
            @RequestParam(required = false, name = "status") OrderStatus status,
            @RequestParam(defaultValue = "0", name = "page", required = false) int page,
            @RequestParam(defaultValue = "10", name = "size", required = false) int size,
            @RequestParam(defaultValue = "DESC", name = "direction", required = false) SortDirections sortDirection) {

        PaginatedResponse<List<OrderResponse>> orders = orderService.getAllOrdersForAdmin(status, page, size, sortDirection);

        APIResponse<PaginatedResponse<List<OrderResponse>>> apiResponse = APIResponse.<PaginatedResponse<List<OrderResponse>>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("All orders retrieved successfully")
                .data(orders)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Admin: Retrieve any order details by ID.
     * Accessible by: ADMIN
     * Returns: 200 OK
     */
    @GetMapping("/admin/{orderId}")
    public ResponseEntity<APIResponse<OrderResponse>> getOrderByIdForAdmin(@PathVariable Long orderId) {
        OrderResponse orderResponse = orderService.getOrderByIdForAdmin(orderId);

        APIResponse<OrderResponse> apiResponse = APIResponse.<OrderResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Admin order details retrieved successfully")
                .data(orderResponse)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Admin: Update the status of any order.
     * Accessible by: ADMIN
     * Returns: 200 OK
     */
    @PatchMapping("/admin/{orderId}/status")
    public ResponseEntity<APIResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam(name = "status") OrderStatus status) {

        OrderResponse orderResponse = orderService.updateOrderStatusByAdmin(orderId, status);

        APIResponse<OrderResponse> apiResponse = APIResponse.<OrderResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Order status updated successfully")
                .data(orderResponse)
                .build();

        return ResponseEntity.ok(apiResponse);
    }
}
