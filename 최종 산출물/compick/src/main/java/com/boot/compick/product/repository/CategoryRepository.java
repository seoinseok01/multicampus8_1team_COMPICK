package com.boot.compick.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.compick.product.entity.CategoryEntity;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
}
