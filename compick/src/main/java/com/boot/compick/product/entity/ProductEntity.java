package com.boot.compick.product.entity;

import java.time.LocalDateTime;

import com.boot.compick.product.SpecJsonSupport;

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

	@Column(name = "stock_quantity", nullable = false)
	private int stockQuantity;

	@Lob
	@Column(name = "product_description")
	private String productDescription;

	@Column(name = "image_url", length = 1000)
	private String imageUrl;

	@Column(name = "sales_status", nullable = false, length = 20)
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

	@Column(name = "gpu_length_mm")
	private Integer gpuLengthMm;

	@Column(name = "max_gpu_length_mm")
	private Integer maxGpuLengthMm;

	@Column(name = "power_capacity_watt")
	private Integer powerCapacityWatt;

	@Lob
	@Column(name = "spec_json")
	private String specJson;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	protected ProductEntity() {
	}

	public static ProductEntity createFromCatalog(CategoryEntity category, String brand, String name,
		long price, int ratingCount, String specJson, String imageUrl) {
		ProductEntity product = new ProductEntity();
		product.category = category;
		product.productName = name;
		product.brand = brand;
		product.modelName = name;
		product.price = price;
		product.ratingCount = ratingCount;
		product.stockQuantity = 10;
		product.productDescription = name + " 상품의 상세 사양과 호환성 정보를 확인할 수 있습니다.";
		product.imageUrl = imageUrl;
		product.salesStatus = "ON_SALE";
		product.socketType = SpecJsonSupport.readText(specJson, "Socket");
		product.memoryType = SpecJsonSupport.readText(specJson, "Memory Type");
		product.formFactor = SpecJsonSupport.readText(specJson, "Form Factor");
		product.powerConsumption = firstInt(specJson, "TDP", "Wattage");
		product.recommendedPower = SpecJsonSupport.readInt(specJson, "Recommended PSU");
		product.gpuLengthMm = SpecJsonSupport.readInt(specJson, "Length");
		product.maxGpuLengthMm = firstInt(specJson, "Maximum Video Card Length", "Max GPU Length");
		product.powerCapacityWatt = SpecJsonSupport.readInt(specJson, "Wattage");
		product.specJson = specJson;
		product.createdAt = LocalDateTime.now();
		return product;
	}

	private static Integer firstInt(String specJson, String... keys) {
		for (String key : keys) {
			Integer value = SpecJsonSupport.readInt(specJson, key);
			if (value != null) return value;
		}
		return null;
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

	public Integer getGpuLengthMm() {
		return gpuLengthMm;
	}

	public Integer getMaxGpuLengthMm() {
		return maxGpuLengthMm;
	}

	public Integer getPowerCapacityWatt() {
		return powerCapacityWatt;
	}

	public String getSpecJson() {
		return specJson;
	}
}
