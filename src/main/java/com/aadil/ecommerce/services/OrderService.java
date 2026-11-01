package com.aadil.ecommerce.services;

import com.aadil.ecommerce.dtos.OrderInCartDTO;
import com.aadil.ecommerce.dtos.OrderRequestDTO;
import com.aadil.ecommerce.entities.OrderEntity;
import com.aadil.ecommerce.entities.OrderItemEntity;
import com.aadil.ecommerce.entities.ProductEntity;
import com.aadil.ecommerce.entities.UserEntity;
import com.aadil.ecommerce.enums.OrderStatus;
import com.aadil.ecommerce.enums.SortDirections;
import com.aadil.ecommerce.enums.UserRole;
import com.aadil.ecommerce.exceptions.InsufficientStockException;
import com.aadil.ecommerce.exceptions.InvalidCredentialException;
import com.aadil.ecommerce.exceptions.InvalidRequestException;
import com.aadil.ecommerce.exceptions.ProductNotFoundException;
import com.aadil.ecommerce.exceptions.ResourceNotFoundException;
import com.aadil.ecommerce.exceptions.UserNotFoundException;
import com.aadil.ecommerce.repositories.OrderRepository;
import com.aadil.ecommerce.repositories.ProductRepository;
import com.aadil.ecommerce.repositories.UserRepository;
import com.aadil.ecommerce.responses.OrderResponse;
import com.aadil.ecommerce.responses.OrderResponseMapper;
import com.aadil.ecommerce.responses.PaginatedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderResponseMapper orderResponseMapper;

    /**
     * Resolves authenticated user and strictly verifies that their role matches the expected role.
     * Prevents cross-role access (User accessing Admin endpoints, or Admin accessing User endpoints).
     */
    private UserEntity getAuthenticatedUserWithRole(UserRole expectedRole) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidCredentialException("Authentication is required to perform this action");
        }

        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            throw new InvalidCredentialException("Authentication principal contains no valid username/email");
        }

        UserEntity user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found: " + email));

        if (expectedRole != null && user.getRole() != expectedRole) {
            throw new AccessDeniedException(String.format(
                    "Access Denied: Only accounts with role '%s' are authorized to perform this operation",
                    expectedRole));
        }

        return user;
    }

    // =========================================================================
    // USER ENDPOINT SERVICE METHODS (Restricted to USER role)
    // =========================================================================

    /**
     * Places a new order from cart items.
     * Strictly restricted to USER role.
     */
    @Transactional
    public OrderResponse createOrder(OrderInCartDTO dto) {
        if (dto == null) {
            throw new InvalidRequestException("Order request payload cannot be null");
        }
        if (dto.getOrderItems() == null || dto.getOrderItems().isEmpty()) {
            throw new InvalidRequestException("Cart must contain at least one order item");
        }

        // Strict Role Check: Customers only
        UserEntity user = getAuthenticatedUserWithRole(UserRole.USER);

        // Validate each item for null or non-positive values
        for (OrderRequestDTO item : dto.getOrderItems()) {
            if (item == null) {
                throw new InvalidRequestException("Order item entry cannot be null");
            }
            if (item.getProductId() == null || item.getProductId() <= 0) {
                throw new InvalidRequestException("Product ID must be a positive non-null number");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new InvalidRequestException("Item quantity must be greater than 0");
            }
            if (item.getQuantity() > 10) {
                throw new InvalidRequestException("Item quantity cannot exceed 10 units per item");
            }
        }

        // 1. Consolidate duplicate items in cart
        Map<Long, Integer> productQuantities = dto.getOrderItems().stream()
                .collect(Collectors.toMap(
                        OrderRequestDTO::getProductId,
                        OrderRequestDTO::getQuantity,
                        Integer::sum
                ));

        // 2. Lock and retrieve products in ordered ID sequence (avoids DB deadlocks)
        List<Long> productIds = new ArrayList<>(productQuantities.keySet());
        Collections.sort(productIds);

        List<ProductEntity> lockedProducts = productRepository.findAllByIdWithPessimisticLock(productIds);
        if (lockedProducts.size() != productIds.size()) {
            throw new ProductNotFoundException("One or more products in your cart could not be found");
        }

        Map<Long, ProductEntity> productMap = lockedProducts.stream()
                .collect(Collectors.toMap(ProductEntity::getProductId, p -> p));

        // 3. Build Order & Items in memory, deduct stock safely
        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setOrderStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            ProductEntity product = productMap.get(entry.getKey());
            int requestedQty = entry.getValue();

            if (product.getStockQuantity() < requestedQty) {
                throw new InsufficientStockException(String.format(
                        "Insufficient stock for '%s'. Available: %d, Requested: %d",
                        product.getProductName(), product.getStockQuantity(), requestedQty));
            }

            product.setStockQuantity(product.getStockQuantity() - requestedQty);

            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(requestedQty));
            totalAmount = totalAmount.add(subtotal);

            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setUnitPrice(unitPrice);
            orderItem.setQuantity(requestedQty);

            order.getOrderItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);

        // 4. Save updated products and cascade-save order with items
        productRepository.saveAll(lockedProducts);
        OrderEntity savedOrder = orderRepository.save(order);

        return orderResponseMapper.mapToResponse(savedOrder);
    }

    /**
     * Retrieves paginated order history for the authenticated customer.
     * Strictly restricted to USER role.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<List<OrderResponse>> getOrderHistory(int page, int size, SortDirections sortDirection) {
        if (page < 0) {
            throw new InvalidRequestException("Page index cannot be negative");
        }
        if (size <= 0) {
            throw new InvalidRequestException("Page size must be greater than 0");
        }
        if (size > 50) {
            throw new InvalidRequestException("Page size cannot exceed 50");
        }

        // Strict Role Check: Customers only
        UserEntity user = getAuthenticatedUserWithRole(UserRole.USER);

        Sort.Direction direction = (sortDirection == SortDirections.ASC)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Sort sort = Sort.by(new Sort.Order(direction, "createdAt"));
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OrderEntity> orderPage = orderRepository.findAllByUser(user, pageable);

        List<OrderResponse> contents = orderPage.getContent().stream()
                .map(orderResponseMapper::mapToResponse)
                .toList();

        return new PaginatedResponse<>(
                contents,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isLast()
        );
    }

    /**
     * Retrieves a single order details by ID for the customer.
     * Strictly restricted to USER role and verifies ownership.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new InvalidRequestException("Order ID must be a positive non-null number");
        }

        // Strict Role Check: Customers only
        UserEntity user = getAuthenticatedUserWithRole(UserRole.USER);

        OrderEntity order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // Strict Ownership Check: Cannot view another customer's order
        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access Denied: You are not authorized to view another user's order");
        }

        return orderResponseMapper.mapToResponse(order);
    }

    /**
     * Cancels an order and restores product stock.
     * Strictly restricted to USER role and verifies ownership.
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new InvalidRequestException("Order ID must be a positive non-null number");
        }

        // Strict Role Check: Customers only
        UserEntity user = getAuthenticatedUserWithRole(UserRole.USER);

        OrderEntity order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // Strict Ownership Check: Cannot cancel another customer's order
        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access Denied: You are not authorized to cancel another user's order");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new InvalidRequestException("Only PENDING orders can be cancelled. Current status is: " + order.getOrderStatus());
        }

        // Restore product inventory stock
        for (OrderItemEntity item : order.getOrderItems()) {
            ProductEntity product = item.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        OrderEntity updatedOrder = orderRepository.save(order);

        return orderResponseMapper.mapToResponse(updatedOrder);
    }

    // =========================================================================
    // ADMIN ENDPOINT SERVICE METHODS (Restricted to ADMIN role)
    // =========================================================================

    /**
     * Admin: Retrieves paginated orders across all users with optional status filter.
     * Strictly restricted to ADMIN role.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<List<OrderResponse>> getAllOrdersForAdmin(
            OrderStatus status, int page, int size, SortDirections sortDirection) {
        if (page < 0) {
            throw new InvalidRequestException("Page index cannot be negative");
        }
        if (size <= 0) {
            throw new InvalidRequestException("Page size must be greater than 0");
        }
        if (size > 100) {
            throw new InvalidRequestException("Page size cannot exceed 100");
        }

        // Strict Role Check: Admins only
        getAuthenticatedUserWithRole(UserRole.ADMIN);

        Sort.Direction direction = (sortDirection == SortDirections.ASC)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Sort sort = Sort.by(new Sort.Order(direction, "createdAt"));
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OrderEntity> orderPage = (status == null)
                ? orderRepository.findAll(pageable)
                : orderRepository.findByOrderStatus(status, pageable);

        List<OrderResponse> contents = orderPage.getContent().stream()
                .map(orderResponseMapper::mapToResponse)
                .toList();

        return new PaginatedResponse<>(
                contents,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isLast()
        );
    }

    /**
     * Admin: Retrieves any order details by ID.
     * Strictly restricted to ADMIN role.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdForAdmin(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new InvalidRequestException("Order ID must be a positive non-null number");
        }

        // Strict Role Check: Admins only
        getAuthenticatedUserWithRole(UserRole.ADMIN);

        OrderEntity order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        return orderResponseMapper.mapToResponse(order);
    }

    /**
     * Admin: Updates order status (e.g., CONFIRMED, SHIPPED, DELIVERED, CANCELLED).
     * Strictly restricted to ADMIN role.
     */
    @Transactional
    public OrderResponse updateOrderStatusByAdmin(Long orderId, OrderStatus newStatus) {
        if (orderId == null || orderId <= 0) {
            throw new InvalidRequestException("Order ID must be a positive non-null number");
        }
        if (newStatus == null) {
            throw new InvalidRequestException("New order status cannot be null");
        }

        // Strict Role Check: Admins only
        getAuthenticatedUserWithRole(UserRole.ADMIN);

        OrderEntity order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        // If transitioning to CANCELLED, restore product inventory
        if (newStatus == OrderStatus.CANCELLED && order.getOrderStatus() != OrderStatus.CANCELLED) {
            for (OrderItemEntity item : order.getOrderItems()) {
                ProductEntity product = item.getProduct();
                if (product != null) {
                    product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                    productRepository.save(product);
                }
            }
        }

        order.setOrderStatus(newStatus);
        OrderEntity updatedOrder = orderRepository.save(order);

        return orderResponseMapper.mapToResponse(updatedOrder);
    }
}
