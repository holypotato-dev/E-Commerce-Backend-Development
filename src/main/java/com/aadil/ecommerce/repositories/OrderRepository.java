package com.aadil.ecommerce.repositories;

import com.aadil.ecommerce.entities.OrderEntity;
import com.aadil.ecommerce.entities.UserEntity;
import com.aadil.ecommerce.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findAllByUser(UserEntity user);

    Page<OrderEntity> findAllByUser(UserEntity user, Pageable pageable);

    Page<OrderEntity> findByOrderStatus(OrderStatus orderStatus, Pageable pageable);

    @Query("SELECT DISTINCT o FROM OrderEntity o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.orderId = :orderId")
    Optional<OrderEntity> findByIdWithItems(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderEntity o WHERE o.orderId = :orderId")
    Optional<OrderEntity> findByIdWithPessimisticLock(@Param("orderId") Long orderId);
}
