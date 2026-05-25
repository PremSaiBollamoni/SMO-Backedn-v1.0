package com.cutm.smo.repositories;

import com.cutm.smo.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByStatus(String status);
    
    @Query("SELECT COALESCE(MAX(p.productId), 0) + 1 FROM Product p")
    Long getNextProductId();
}
