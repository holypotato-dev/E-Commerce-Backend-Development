package com.aadil.ecommerce.repositories;

import com.aadil.ecommerce.entities.ProductEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    Page<ProductEntity> findByProductNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String productName, String description, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.productId IN :ids")
    List<ProductEntity> findAllByIdWithPessimisticLock(@Param("ids") List<Long> ids);
}
