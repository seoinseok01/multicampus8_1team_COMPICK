package com.boot.compick.product.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.boot.compick.product.entity.CategoryEntity;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    Optional<CategoryEntity> findByCategoryName(String categoryName);
}
