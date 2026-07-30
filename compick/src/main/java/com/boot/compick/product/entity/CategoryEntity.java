package com.boot.compick.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "CATEGORY")
public class CategoryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "category_id")
	private Long categoryId;

	@Column(name = "category_name", nullable = false, unique = true, length = 30)
	private String categoryName;

	protected CategoryEntity() {
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}
}
