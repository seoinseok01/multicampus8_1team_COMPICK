document.addEventListener("DOMContentLoaded", () => {
	const grid = document.querySelector("[data-product-grid]");
	if (!grid) {
		return;
	}

	const categoryTabs = document.querySelectorAll(".category-tabs button");
	const categoryLabel = document.querySelector("[data-category-label]");
	const productCount = document.querySelector("[data-product-count]");
	const brandList = document.querySelector("[data-brand-list]");
	const specList = document.querySelector("[data-spec-list]");
	const minPriceRange = document.querySelector("[data-min-price-range]");
	const maxPriceRange = document.querySelector("[data-max-price-range]");
	const priceSliderRange = document.querySelector("[data-price-slider-range]");
	const minPriceLabel = document.querySelector("[data-min-price-label]");
	const maxPriceLabel = document.querySelector("[data-max-price-label]");
	const applyFilterButton = document.querySelector("[data-apply-filter]");
	const resetFilterButton = document.querySelector("[data-reset-filter]");
	const sortButtons = document.querySelectorAll("[data-sort]");
	const storageTypeToggle = document.querySelector("[data-storage-type-toggle]");
	const storageTypeButtons = document.querySelectorAll("[data-storage-type]");
	const emptyMessage = document.querySelector("[data-empty-message]");
	const scrollSentinel = document.querySelector("[data-scroll-sentinel]");
	const loadingIndicator = document.querySelector("[data-loading-indicator]");
	const feedback = document.querySelector("#cart-feedback");

	const activeTab = document.querySelector(".category-tabs button.is-active");

	const state = {
		category: activeTab?.dataset.category ?? "CPU",
		brands: [],
		minPrice: null,
		maxPrice: null,
		storageType: "",
		sort: "popular",
		specFilters: {},
		page: Number(grid.dataset.initialPage ?? 0),
		size: 20,
		hasNext: grid.dataset.initialHasNext === "true",
		loading: false
	};

	let feedbackTimer = null;
	let priceSliderTimer = null;

	const formatPrice = (price) =>
		new Intl.NumberFormat("ko-KR").format(Number(price)) + "원";

	const showFeedback = (message, isError = false) => {
		if (!feedback) {
			return;
		}
		window.clearTimeout(feedbackTimer);
		feedback.textContent = message;
		feedback.classList.toggle("is-error", isError);
		feedback.hidden = false;
		feedbackTimer = window.setTimeout(() => {
			feedback.hidden = true;
		}, 3000);
	};

	const csrfHeaders = () => {
		const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
		const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
		const headers = { "Content-Type": "application/json", Accept: "application/json" };
		if (csrfToken && csrfHeader) {
			headers[csrfHeader] = csrfToken;
		}
		return headers;
	};

	const addToCart = async (productId, button) => {
		if (!productId || button?.disabled) {
			return;
		}
		if (button) {
			button.disabled = true;
		}
		try {
			const response = await fetch("/api/cart/items", {
				method: "POST",
				headers: csrfHeaders(),
				body: JSON.stringify({ productId, quantity: 1 })
			});

			if (response.status === 401 || response.redirected) {
				window.location.assign("/login");
				return;
			}

			const result = await response.json().catch(() => ({}));
			if (!response.ok) {
				throw new Error(result.message || result.detail || "장바구니에 담지 못했습니다.");
			}
			showFeedback(`장바구니에 담았습니다. 총 ${result.cartItemCount}개`);
			if (window.confirm("상품이 장바구니에 담겼습니다.\n장바구니로 이동하시겠습니까?")) {
				window.location.assign("/cart");
			}
		} catch (error) {
			showFeedback(error.message, true);
		} finally {
			if (button) {
				button.disabled = false;
			}
		}
	};

	const renderCard = (product) => {
		const li = document.createElement("li");
		li.className = "product-grid-item";

		const image = document.createElement("img");
		image.className = "product-image";
		image.loading = "lazy";
		image.decoding = "async";
		image.referrerPolicy = "no-referrer";
		image.width = 212;
		image.height = 132;
		image.alt = `${product.name} 상품 이미지`;
		image.dataset.fallbackSrc = "/images/products/product-placeholder.svg";
		image.src = product.imageUrl && product.imageUrl.length > 0
			? product.imageUrl
			: "/images/products/product-placeholder.svg";
		image.addEventListener("error", () => {
			if (image.src.endsWith("product-placeholder.svg")) {
				return;
			}
			image.src = image.dataset.fallbackSrc;
			image.classList.add("is-fallback");
		}, { once: true });

		const brand = document.createElement("p");
		brand.textContent = product.brand;

		const name = document.createElement("h3");
		name.textContent = product.name;

		const price = document.createElement("strong");
		price.textContent = formatPrice(product.price);

		const button = document.createElement("button");
		button.type = "button";
		button.className = "add-cart-button";
		button.textContent = "담기";
		button.dataset.productId = product.productId;
		button.disabled = product.stockQuantity <= 0;
		button.addEventListener("click", () => addToCart(product.productId, button));

		li.append(image, brand, name, price, button);
		return li;
	};

	const buildQuery = (page) => {
		const params = new URLSearchParams();
		params.set("category", state.category);
		params.set("sort", state.sort);
		params.set("page", String(page));
		params.set("size", String(state.size));
		state.brands.forEach((brand) => params.append("brands", brand));
		if (state.minPrice != null) {
			params.set("minPrice", String(state.minPrice));
		}
		if (state.maxPrice != null) {
			params.set("maxPrice", String(state.maxPrice));
		}
		if (state.storageType) {
			params.set("storageType", state.storageType);
		}
		Object.entries(state.specFilters).forEach(([key, value]) => {
			if (value) {
				params.set(`spec_${key.replace(/ /g, "_")}`, value);
			}
		});
		return params.toString();
	};

	const fetchProducts = async (reset) => {
		if (state.loading) {
			return;
		}
		if (!reset && !state.hasNext) {
			return;
		}

		state.loading = true;
		if (loadingIndicator) {
			loadingIndicator.hidden = false;
		}
		const pageToFetch = reset ? 0 : state.page + 1;

		try {
			const response = await fetch(`/api/products?${buildQuery(pageToFetch)}`);
			if (!response.ok) {
				throw new Error("상품을 불러오지 못했습니다.");
			}
			const pageResult = await response.json();

			if (reset) {
				grid.innerHTML = "";
			}

			pageResult.content.forEach((product) => grid.appendChild(renderCard(product)));

			state.hasNext = !pageResult.last;
			state.page = pageResult.number;

			if (productCount) {
				productCount.textContent = `${pageResult.totalElements}개`;
			}
			if (emptyMessage) {
				emptyMessage.hidden = pageResult.totalElements > 0;
			}
		} catch (error) {
			showFeedback(error.message, true);
		} finally {
			state.loading = false;
			if (loadingIndicator) {
				loadingIndicator.hidden = true;
			}
		}
	};

	const updatePriceSliderVisual = () => {
		if (!minPriceRange || !maxPriceRange || !priceSliderRange) {
			return;
		}
		const min = Number(minPriceRange.min);
		const max = Number(minPriceRange.max);
		const span = max - min || 1;
		const left = ((Number(minPriceRange.value) - min) / span) * 100;
		const right = ((Number(maxPriceRange.value) - min) / span) * 100;
		priceSliderRange.style.left = `${left}%`;
		priceSliderRange.style.right = `${100 - right}%`;
		if (minPriceLabel) minPriceLabel.textContent = formatPrice(minPriceRange.value);
		if (maxPriceLabel) maxPriceLabel.textContent = formatPrice(maxPriceRange.value);
	};

	const setupPriceSlider = (minPrice, maxPrice) => {
		if (!minPriceRange || !maxPriceRange) {
			return;
		}
		const lower = Math.floor(minPrice);
		const upper = Math.max(Math.ceil(maxPrice), lower + 1);
		[minPriceRange, maxPriceRange].forEach((input) => {
			input.min = String(lower);
			input.max = String(upper);
			input.step = "1000";
		});
		minPriceRange.value = String(lower);
		maxPriceRange.value = String(upper);
		state.minPrice = lower;
		state.maxPrice = upper;
		updatePriceSliderVisual();
	};

	const onPriceSliderInput = () => {
		if (!minPriceRange || !maxPriceRange) {
			return;
		}
		if (Number(minPriceRange.value) > Number(maxPriceRange.value)) {
			const swapTarget = document.activeElement === minPriceRange ? maxPriceRange : minPriceRange;
			swapTarget.value = document.activeElement === minPriceRange
				? minPriceRange.value
				: maxPriceRange.value;
		}
		state.minPrice = Number(minPriceRange.value);
		state.maxPrice = Number(maxPriceRange.value);
		updatePriceSliderVisual();

		window.clearTimeout(priceSliderTimer);
		priceSliderTimer = window.setTimeout(() => {
			resetAndReload();
		}, 250);
	};

	minPriceRange?.addEventListener("input", onPriceSliderInput);
	maxPriceRange?.addEventListener("input", onPriceSliderInput);

	storageTypeButtons.forEach((button) => {
		button.addEventListener("click", () => {
			storageTypeButtons.forEach((b) => b.classList.toggle("is-active", b === button));
			state.storageType = button.dataset.storageType;
			resetAndReload();
		});
	});

	const loadFacets = async (category) => {
		if (!brandList || !specList) {
			return;
		}
		brandList.innerHTML = "";
		specList.innerHTML = "";

		if (storageTypeToggle) {
			storageTypeToggle.hidden = category !== "STORAGE";
			state.storageType = "";
			storageTypeButtons.forEach((b) => b.classList.toggle("is-active", b.dataset.storageType === ""));
		}

		try {
			const response = await fetch(`/api/products/${category}/facets`);
			if (!response.ok) {
				return;
			}
			const facets = await response.json();

			setupPriceSlider(facets.minPrice, facets.maxPrice);

			facets.brands.forEach((brand) => {
				const label = document.createElement("label");
				label.className = "brand-checkbox";
				const checkbox = document.createElement("input");
				checkbox.type = "checkbox";
				checkbox.value = brand;
				label.append(checkbox, document.createTextNode(brand));
				brandList.appendChild(label);
			});

			Object.entries(facets.specOptions).forEach(([key, values]) => {
				if (!values || values.length === 0) {
					return;
				}
				const select = document.createElement("select");
				select.dataset.specKey = key;

				const defaultOption = document.createElement("option");
				defaultOption.value = "";
				defaultOption.textContent = key;
				select.appendChild(defaultOption);

				values.forEach((value) => {
					const option = document.createElement("option");
					option.value = value;
					option.textContent = value;
					select.appendChild(option);
				});

				specList.appendChild(select);
			});
		} catch (error) {
			// 필터 옵션은 부가 기능이라 실패해도 상품 목록 조회는 계속한다
		}
	};

	const resetAndReload = () => {
		state.page = 0;
		state.hasNext = true;
		fetchProducts(true);
	};

	categoryTabs.forEach((tab) => {
		tab.addEventListener("click", () => {
			categoryTabs.forEach((t) => t.classList.toggle("is-active", t === tab));
			state.category = tab.dataset.category;
			state.brands = [];
			state.minPrice = null;
			state.maxPrice = null;
			state.specFilters = {};
			if (categoryLabel) categoryLabel.textContent = tab.textContent.trim();
			loadFacets(state.category);
			resetAndReload();
		});
	});

	sortButtons.forEach((button) => {
		button.addEventListener("click", () => {
			sortButtons.forEach((b) => b.classList.toggle("is-active", b === button));
			state.sort = button.dataset.sort;
			resetAndReload();
		});
	});

	applyFilterButton?.addEventListener("click", () => {
		state.brands = Array.from(brandList?.querySelectorAll("input:checked") ?? [])
			.map((input) => input.value);
		state.specFilters = {};
		specList?.querySelectorAll("select").forEach((select) => {
			if (select.value) {
				state.specFilters[select.dataset.specKey] = select.value;
			}
		});
		resetAndReload();
	});

	resetFilterButton?.addEventListener("click", () => {
		state.brands = [];
		state.specFilters = {};
		if (minPriceRange && maxPriceRange) {
			setupPriceSlider(Number(minPriceRange.min), Number(minPriceRange.max));
		}
		brandList?.querySelectorAll("input").forEach((input) => { input.checked = false; });
		specList?.querySelectorAll("select").forEach((select) => { select.value = ""; });
		resetAndReload();
	});

	document.querySelectorAll(".product-grid .add-cart-button").forEach((button) => {
		button.addEventListener("click", () => {
			addToCart(Number(button.dataset.productId), button);
		});
	});

	if (scrollSentinel && "IntersectionObserver" in window) {
		const observer = new IntersectionObserver((entries) => {
			entries.forEach((entry) => {
				if (entry.isIntersecting) {
					fetchProducts(false);
				}
			});
		}, { rootMargin: "200px" });
		observer.observe(scrollSentinel);
	}

	loadFacets(state.category);
});
