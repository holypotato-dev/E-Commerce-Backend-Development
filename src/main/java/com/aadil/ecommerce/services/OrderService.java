package com.aadil.ecommerce.services;

import com.aadil.ecommerce.dtos.OrderInCartDTO;
import com.aadil.ecommerce.dtos.OrderRequestDTO;
import com.aadil.ecommerce.entities.OrderEntity;
import com.aadil.ecommerce.entities.OrderItemEntity;
import com.aadil.ecommerce.entities.ProductEntity;
import com.aadil.ecommerce.entities.UserEntity;
import com.aadil.ecommerce.enums.OrderStatus;
import com.aadil.ecommerce.exceptions.InsufficientStockException;
import com.aadil.ecommerce.exceptions.InvalidRequestException;
import com.aadil.ecommerce.exceptions.ProductNotFoundException;
import com.aadil.ecommerce.exceptions.UserNotFoundException;
import com.aadil.ecommerce.repositories.OrderRepository;
import com.aadil.ecommerce.repositories.ProductRepository;
import com.aadil.ecommerce.repositories.UserRepository;
import com.aadil.ecommerce.responses.OrderResponse;
import com.aadil.ecommerce.responses.OrderResponseMapper;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public OrderResponse createOrder(OrderInCartDTO dto) {
        if (dto.getOrderItems() == null || dto.getOrderItems().isEmpty()) {
            throw new InvalidRequestException("Cart must contain at least one item");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidRequestException("User must be authenticated to place an order");
        }

        String email = authentication.getName().trim().toLowerCase();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        // 1. Consolidate duplicate products in cart (prevent duplicate processing)
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
            throw new ProductNotFoundException("One or more products in the cart no longer exist");
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

            // Deduct stock within locked transaction
            product.setStockQuantity(product.getStockQuantity() - requestedQty);

            BigDecimal unitPrice = product.getPrice();
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(requestedQty));
            totalAmount = totalAmount.add(itemTotal);

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
}
