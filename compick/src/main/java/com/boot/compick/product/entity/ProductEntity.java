package com.boot.compick.product.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "PRODUCT")
public class ProductEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_id")
	private Long productId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private CategoryEntity category;

	@Column(name = "product_name", nullable = false, length = 255)
	private String productName;

	@Column(name = "brand", nullable = false, length = 100)
	private String brand;

	@Column(name = "model_name", nullable = false, length = 150)
	private String modelName;

	@Column(name = "price", nullable = false)
	private long price;

	@Column(name = "rating_count", nullable = false)
	private int ratingCount;

<<<<<<< HEAD
	@Column(name = "stock_quantity", nullable = false, columnDefinition = "NUMBER(10) DEFAULT 0")
=======
	@Column(name = "stock_quantity", nullable = false)
>>>>>>> 48ad55d3c2f8342386c89a8e9f5dff696b5a09ad
	private int stockQuantity;

	@Lob
	@Column(name = "product_description")
	private String productDescription;

	@Column(name = "image_url", length = 1000)
	private String imageUrl;

<<<<<<< HEAD
	@Column(name = "sales_status", nullable = false, length = 20, columnDefinition = "VARCHAR2(20 CHAR) DEFAULT 'ON_SALE'")
=======
	@Column(name = "sales_status", nullable = false, length = 20)
>>>>>>> 48ad55d3c2f8342386c89a8e9f5dff696b5a09ad
	private String salesStatus;

	@Column(name = "socket_type", length = 50)
	private String socketType;

	@Column(name = "memory_type", length = 50)
	private String memoryType;

	@Column(name = "form_factor", length = 50)
	private String formFactor;

	@Column(name = "power_consumption")
	private Integer powerConsumption;

	@Column(name = "recommended_power")
	private Integer recommendedPower;

	@Lob
	@Column(name = "spec_json")
	private String specJson;

<<<<<<< HEAD
	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT SYSTIMESTAMP")
=======
	@Column(name = "created_at", nullable = false)
>>>>>>> 48ad55d3c2f8342386c89a8e9f5dff696b5a09ad
	private LocalDateTime createdAt;

	protected ProductEntity() {
	}

	public Long getProductId() {
		return productId;
	}

	public CategoryEntity getCategory() {
		return category;
	}

	public String getProductName() {
		return productName;
	}

	public String getBrand() {
		return brand;
	}

	public String getModelName() {
		return modelName;
	}

	public long getPrice() {
		return price;
	}

	public int getRatingCount() {
		return ratingCount;
	}

	public int getStockQuantity() {
		return stockQuantity;
	}

	public String getProductDescription() {
		return productDescription;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getSalesStatus() {
		return salesStatus;
	}

	public String getSocketType() {
		return socketType;
	}

	public String getMemoryType() {
		return memoryType;
	}

	public String getFormFactor() {
		return formFactor;
	}

	public Integer getPowerConsumption() {
		return powerConsumption;
	}

	public Integer getRecommendedPower() {
		return recommendedPower;
	}

	public String getSpecJson() {
		return specJson;
	}
}
