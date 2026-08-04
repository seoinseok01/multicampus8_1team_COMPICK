const SPEC_LABELS = {
	"Socket": "소켓",
	"Core Count": "코어 수",
	"Memory Type": "메모리 규격",
	"Form Factor": "폼팩터",
	"Chipset": "칩셋",
	"Memory": "메모리",
	"Speed": "속도",
	"Type": "유형",
	"Wattage": "정격 출력",
	"Efficiency": "인증 등급"
};

document.addEventListener("DOMContentLoaded", () => {
	const categoryButtons = document.querySelectorAll(".category-tabs button[data-category]");
	if (categoryButtons.length === 0) {
		return;
	}

	const productGrid = document.querySelector("[data-quote-product-grid]");
	const categoryTitle = document.querySelector("[data-selected-category-title]");
	const sortButtons = document.querySelectorAll(".quote-product-panel [data-sort]");
	const storageTypeToggle = document.querySelector("[data-storage-type-toggle]");
	const storageTypeButtons = document.querySelectorAll("[data-storage-type]");
	const loadingIndicator = document.querySelector("[data-loading-indicator]");
	const summaryList = document.querySelector("[data-summary-list]");
	const compatibilityBanner = document.querySelector("[data-compatibility-banner]");
	const estimatedPowerElement = document.querySelector("[data-estimated-power]");
	const totalPriceElement = document.querySelector("[data-total-price]");
	const submitButton = document.querySelector("[data-submit-quote]");
	const summaryPanel = document.querySelector(".quote-summary-panel");
	const summaryContainer = document.querySelector(".quote-builder-layout");
	const assemblyOptions = document.querySelectorAll("[data-assembly-option]");
	const feedback = document.querySelector("#cart-feedback");
	const brandList = document.querySelector("[data-brand-list]");
	const specList = document.querySelector("[data-spec-list]");
	const minPriceRange = document.querySelector("[data-min-price-range]");
	const maxPriceRange = document.querySelector("[data-max-price-range]");
	const priceSliderRange = document.querySelector("[data-price-slider-range]");
	const minPriceLabel = document.querySelector("[data-min-price-label]");
	const maxPriceLabel = document.querySelector("[data-max-price-label]");
	const applyFilterButton = document.querySelector("[data-apply-filter]");
	const resetFilterButton = document.querySelector("[data-reset-filter]");

	const ASSEMBLY_FEE = 30000;
	const RAM_CATEGORY = "RAM";
	const MAINBOARD_CATEGORY = "MAINBOARD";
	const categories = Array.from(categoryButtons).map((button) => ({
		name: button.dataset.category,
		label: button.textContent.trim()
	}));

	// 카테고리별 단일 선택 상품(또는 RAM은 { ...product, quantity } 배열)을 보관한다.
	const selected = {};
	// 상세 팝업에서 담기/추가를 누를 때 호환성 검사에 필요한 원본 필드(소켓 등)를 잃지 않도록 보관한다.
	const productsById = new Map();
	let currentCategory = categories[0].name;
	let sort = "popular";
	let storageType = "";
	let keyword = "";
	let brands = [];
	let minPrice = null;
	let maxPrice = null;
	let specFilters = {};
	let feedbackTimer = null;
	let priceSliderTimer = null;

	const formatPrice = (price) => new Intl.NumberFormat("ko-KR").format(Number(price)) + "원";

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

	const setupPriceSlider = (rawMinPrice, rawMaxPrice) => {
		if (!minPriceRange || !maxPriceRange) {
			return;
		}
		const lower = Math.floor(rawMinPrice / 1000) * 1000;
		const upper = Math.max(Math.ceil(rawMaxPrice / 1000) * 1000, lower + 1000);
		[minPriceRange, maxPriceRange].forEach((input) => {
			input.min = String(lower);
			input.max = String(upper);
			input.step = "1000";
		});
		minPriceRange.value = String(lower);
		maxPriceRange.value = String(upper);
		minPrice = lower;
		maxPrice = upper;
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
		minPrice = Number(minPriceRange.value);
		maxPrice = Number(maxPriceRange.value);
		updatePriceSliderVisual();

		window.clearTimeout(priceSliderTimer);
		priceSliderTimer = window.setTimeout(() => {
			fetchCategoryProducts();
		}, 250);
	};

	minPriceRange?.addEventListener("input", onPriceSliderInput);
	maxPriceRange?.addEventListener("input", onPriceSliderInput);

	const loadFacets = async (category) => {
		if (storageTypeToggle) {
			storageTypeToggle.hidden = category !== "STORAGE";
			storageType = "";
			storageTypeButtons.forEach((b) => b.classList.toggle("is-active", b.dataset.storageType === ""));
		}
		if (!brandList || !specList) {
			return;
		}
		brandList.innerHTML = "";
		specList.innerHTML = "";

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
				defaultOption.textContent = SPEC_LABELS[key] ?? key;
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

	applyFilterButton?.addEventListener("click", () => {
		brands = Array.from(brandList?.querySelectorAll("input:checked") ?? [])
			.map((input) => input.value);
		specFilters = {};
		specList?.querySelectorAll("select").forEach((select) => {
			if (select.value) {
				specFilters[select.dataset.specKey] = select.value;
			}
		});
		fetchCategoryProducts();
	});

	resetFilterButton?.addEventListener("click", () => {
		brands = [];
		specFilters = {};
		if (minPriceRange && maxPriceRange) {
			setupPriceSlider(Number(minPriceRange.min), Number(minPriceRange.max));
		}
		brandList?.querySelectorAll("input").forEach((input) => { input.checked = false; });
		specList?.querySelectorAll("select").forEach((select) => { select.value = ""; });
		fetchCategoryProducts();
	});

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

	const isCategorySelected = (categoryName) =>
		categoryName === RAM_CATEGORY
			? Boolean(selected.RAM && selected.RAM.length > 0)
			: Boolean(selected[categoryName]);

	const ramSlotLimit = () => selected.MAINBOARD?.memorySlots ?? null;

	const ramTotalQuantity = () =>
		(selected.RAM || []).reduce((sum, item) => sum + item.quantity, 0);

	const loadInitialItems = () => {
		try {
			const items = JSON.parse(window.__QUOTE_INITIAL_ITEMS__ ?? "[]");
			items.forEach((item) => {
				if (item.category === RAM_CATEGORY) {
					if (!selected.RAM) {
						selected.RAM = [];
					}
					selected.RAM.push({ ...item, quantity: item.quantity ?? 1 });
				} else {
					selected[item.category] = item;
				}
			});
		} catch (error) {
			// 초기 프리필 데이터가 없거나 잘못돼도 빈 견적으로 시작한다
		}
	};

	const addRamItem = (product) => {
		const ramList = selected.RAM || (selected.RAM = []);
		const maxSlots = ramSlotLimit();
		if (maxSlots != null && ramTotalQuantity() + 1 > maxSlots) {
			window.alert(`RAM은 최대 ${maxSlots}개까지 선택할 수 있습니다.`);
			return;
		}
		const existing = ramList.find((item) => item.productId === product.productId);
		if (existing) {
			existing.quantity += 1;
		} else {
			ramList.push({ ...product, quantity: 1 });
		}
		fetchCategoryProducts();
		renderSummary();
	};

	const changeRamQuantity = (productId, delta) => {
		const ramList = selected.RAM || [];
		const item = ramList.find((entry) => entry.productId === productId);
		if (!item) {
			return;
		}
		if (delta > 0) {
			const maxSlots = ramSlotLimit();
			if (maxSlots != null && ramTotalQuantity() + 1 > maxSlots) {
				window.alert(`RAM은 최대 ${maxSlots}개까지 선택할 수 있습니다.`);
				return;
			}
		}
		item.quantity += delta;
		if (item.quantity <= 0) {
			selected.RAM = ramList.filter((entry) => entry.productId !== productId);
		}
		if (currentCategory === RAM_CATEGORY) {
			fetchCategoryProducts();
		}
		renderSummary();
	};

	const removeRamItem = (productId) => {
		selected.RAM = (selected.RAM || []).filter((entry) => entry.productId !== productId);
		if (currentCategory === RAM_CATEGORY) {
			fetchCategoryProducts();
		}
		renderSummary();
	};

	const enforceRamSlotLimit = () => {
		const maxSlots = ramSlotLimit();
		if (maxSlots == null || !selected.RAM || selected.RAM.length === 0) {
			return;
		}
		if (ramTotalQuantity() > maxSlots) {
			selected.RAM = [];
			window.alert(
				`메인보드 변경으로 RAM 선택이 초기화되었습니다. RAM은 최대 ${maxSlots}개까지 선택할 수 있습니다.`
			);
		}
	};

	const selectItem = (product) => {
		selected[product.category] = product;
		if (product.category === MAINBOARD_CATEGORY) {
			enforceRamSlotLimit();
		}
		fetchCategoryProducts();
		renderSummary();
	};

	const renderProductCard = (product) => {
		const li = document.createElement("li");
		li.className = "product-grid-item";
		li.dataset.productId = product.productId;

		const isRam = product.category === RAM_CATEGORY;
		const ramLine = isRam
			? (selected.RAM || []).find((item) => item.productId === product.productId)
			: null;
		const isSingleSelected = !isRam && selected[product.category]?.productId === product.productId;
		if (isSingleSelected || ramLine) {
			li.classList.add("is-selected");
		}

		const image = document.createElement("img");
		image.className = "product-image";
		image.loading = "lazy";
		image.decoding = "async";
		image.referrerPolicy = "no-referrer";
		image.width = 212;
		image.height = 132;
		image.alt = `${product.name} 상품 이미지`;
		image.src = product.imageUrl && product.imageUrl.length > 0
			? product.imageUrl
			: "/images/products/product-placeholder.svg";
		image.addEventListener("error", () => {
			image.src = "/images/products/product-placeholder.svg";
		}, { once: true });

		const brand = document.createElement("p");
		brand.textContent = product.brand;

		const name = document.createElement("h3");
		name.textContent = product.name;

		const specLine = document.createElement("p");
		specLine.className = "product-spec-line";
		specLine.textContent = Object.values(product.specs ?? {}).join(" / ");

		const price = document.createElement("strong");
		price.textContent = formatPrice(product.price);

		const button = document.createElement("button");
		button.type = "button";
		if (isRam) {
			button.textContent = ramLine ? `추가 (담김 ${ramLine.quantity}개)` : "추가";
		} else {
			button.textContent = isSingleSelected ? "선택됨" : "담기";
		}
		button.disabled = product.stockQuantity <= 0;
		button.addEventListener("click", () => {
			if (isRam) {
				addRamItem(product);
			} else {
				selectItem(product);
			}
		});

		li.append(image, brand, name, specLine, price, button);
		return li;
	};

	const fetchCategoryProducts = async () => {
		if (!productGrid) {
			return;
		}
		if (loadingIndicator) {
			loadingIndicator.hidden = false;
		}
		try {
			const params = new URLSearchParams({
				category: currentCategory,
				sort,
				page: "0",
				size: "20"
			});
			if (currentCategory === "STORAGE" && storageType) {
				params.set("storageType", storageType);
			}
			if (keyword) {
				params.set("keyword", keyword);
			}
			brands.forEach((brand) => params.append("brands", brand));
			if (minPrice != null) {
				params.set("minPrice", String(minPrice));
			}
			if (maxPrice != null) {
				params.set("maxPrice", String(maxPrice));
			}
			Object.entries(specFilters).forEach(([key, value]) => {
				if (value) {
					params.set(`spec_${key.replace(/ /g, "_")}`, value);
				}
			});
			const response = await fetch(`/api/products?${params.toString()}`);
			if (!response.ok) {
				throw new Error("상품을 불러오지 못했습니다.");
			}
			const pageResult = await response.json();
			productGrid.innerHTML = "";
			pageResult.content.forEach((product) => {
				productsById.set(product.productId, product);
				productGrid.appendChild(renderProductCard(product));
			});
		} catch (error) {
			showFeedback(error.message, true);
		} finally {
			if (loadingIndicator) {
				loadingIndicator.hidden = true;
			}
		}
	};

	const assemblyFee = () => {
		const checked = document.querySelector("[data-assembly-option]:checked");
		return checked?.value === "ASSEMBLED" ? ASSEMBLY_FEE : 0;
	};

	/* ---- 호환성 검사: 소켓 / 메모리 규격 / 폼팩터 / GPU 길이 / 파워 용량 ---- */

	const FORM_FACTOR_RANKS = [
		{ rank: 5, patterns: ["E-ATX", "EATX"] },
		{ rank: 3, patterns: ["MICRO", "MATX", "M-ATX"] },
		{ rank: 2, patterns: ["DTX"] },
		{ rank: 1, patterns: ["ITX"] },
		{ rank: 4, patterns: ["ATX"] }
	];

	const formFactorRank = (text) => {
		if (!text) {
			return null;
		}
		const upper = text.toUpperCase();
		const matched = FORM_FACTOR_RANKS.find(({ patterns }) => patterns.some((p) => upper.includes(p)));
		return matched ? matched.rank : null;
	};

	const evaluateCompatibility = () => {
		const issues = [];
		const cpu = selected.CPU;
		const board = selected.MAINBOARD;
		const ramList = selected.RAM || [];
		const gpu = selected.GPU;
		const psu = selected.POWER_SUPPLY;
		const pcCase = selected.CASE;

		if (cpu && board && cpu.socketType && board.socketType && cpu.socketType !== board.socketType) {
			issues.push(`CPU 소켓(${cpu.socketType})과 메인보드 소켓(${board.socketType})이 일치하지 않습니다.`);
		}

		if (board && board.memoryType) {
			ramList.forEach((ram) => {
				if (ram.memoryType && ram.memoryType !== board.memoryType) {
					issues.push(`RAM "${ram.name}"의 메모리 규격(${ram.memoryType})이 메인보드(${board.memoryType})와 다릅니다.`);
				}
			});
		}

		if (board && pcCase) {
			const boardRank = formFactorRank(board.formFactor);
			const caseRank = formFactorRank(pcCase.formFactor);
			if (boardRank != null && caseRank != null && caseRank < boardRank) {
				issues.push(`케이스(${pcCase.formFactor})가 메인보드(${board.formFactor})보다 작아 장착할 수 없습니다.`);
			}
		}

		if (gpu && pcCase && gpu.gpuLengthMm != null && pcCase.maxGpuLengthMm != null) {
			if (gpu.gpuLengthMm > pcCase.maxGpuLengthMm) {
				issues.push(`그래픽카드 길이(${gpu.gpuLengthMm}mm)가 케이스 최대 장착 길이(${pcCase.maxGpuLengthMm}mm)를 초과합니다.`);
			}
		}

		if (psu && psu.powerCapacityWatt != null && (cpu?.powerConsumption != null || gpu?.recommendedPower != null)) {
			const required = Math.ceil(((cpu?.powerConsumption ?? 0) + (gpu?.recommendedPower ?? 0)) * 1.2);
			if (required > psu.powerCapacityWatt) {
				issues.push(`예상 소비전력 기준 권장 파워(약 ${required}W)가 파워 용량(${psu.powerCapacityWatt}W)을 초과합니다.`);
			}
		}

		return issues;
	};

	const renderCompatibilityBanner = (issues, allSelected) => {
		if (!compatibilityBanner) {
			return;
		}
		compatibilityBanner.innerHTML = "";

		if (issues.length > 0) {
			compatibilityBanner.className = "compatibility-banner is-fail";
			const heading = document.createElement("strong");
			heading.textContent = `⚠ 호환성 문제 ${issues.length}건`;
			compatibilityBanner.appendChild(heading);
			const list = document.createElement("ul");
			issues.forEach((issue) => {
				const li = document.createElement("li");
				li.textContent = issue;
				list.appendChild(li);
			});
			compatibilityBanner.appendChild(list);
			return;
		}

		if (allSelected) {
			compatibilityBanner.className = "compatibility-banner is-pass";
			const heading = document.createElement("strong");
			heading.textContent = "✓ 호환성 검사 통과";
			const span = document.createElement("span");
			span.textContent = "소켓·메모리 규격·폼팩터·GPU 길이·파워 용량을 확인했습니다.";
			compatibilityBanner.append(heading, span);
			return;
		}

		compatibilityBanner.className = "compatibility-banner";
		const heading = document.createElement("strong");
		heading.textContent = "호환성 검사 대기 중";
		const span = document.createElement("span");
		span.textContent = "부품을 선택할 때마다 관련된 다른 부품과의 호환성을 바로 검사합니다.";
		compatibilityBanner.append(heading, span);
	};

	/* ---- 내 견적서 요약 ---- */

	const renderRamSummaryLine = (item, showLabel, label) => {
		const li = document.createElement("li");
		li.className = "ram-summary-line";

		const labelSpan = document.createElement("span");
		labelSpan.textContent = showLabel ? label : "";

		const value = document.createElement("span");
		value.className = "ram-summary-value";

		const name = document.createElement("span");
		name.textContent = `${item.name} x${item.quantity}`;

		const stepper = document.createElement("span");
		stepper.className = "ram-stepper";

		const minusButton = document.createElement("button");
		minusButton.type = "button";
		minusButton.textContent = "−";
		minusButton.setAttribute("aria-label", `${item.name} 수량 줄이기`);
		minusButton.addEventListener("click", () => changeRamQuantity(item.productId, -1));

		const plusButton = document.createElement("button");
		plusButton.type = "button";
		plusButton.textContent = "+";
		plusButton.setAttribute("aria-label", `${item.name} 수량 늘리기`);
		plusButton.addEventListener("click", () => changeRamQuantity(item.productId, 1));

		const removeButton = document.createElement("button");
		removeButton.type = "button";
		removeButton.className = "ram-remove";
		removeButton.textContent = "✕";
		removeButton.setAttribute("aria-label", `${item.name} 삭제`);
		removeButton.addEventListener("click", () => removeRamItem(item.productId));

		stepper.append(minusButton, plusButton, removeButton);
		value.append(name, stepper);
		li.append(labelSpan, value);
		return li;
	};

	/*
	 * 견적서 패널이 뷰포트보다 길 때, 별도 내부 스크롤 없이 페이지 스크롤과 함께
	 * 아래쪽까지 자연스럽게 드러나도록 top 오프셋을 스크롤에 맞춰 동적으로 조정한다.
	 */
	const SUMMARY_HEADER_GAP = 96;
	const SUMMARY_BOTTOM_GAP = 24;

	const updateSummarySticky = () => {
		if (!summaryPanel || !summaryContainer) {
			return;
		}
		const containerRect = summaryContainer.getBoundingClientRect();
		const panelHeight = summaryPanel.offsetHeight;
		const availableHeight = window.innerHeight - SUMMARY_HEADER_GAP - SUMMARY_BOTTOM_GAP;
		const overflow = panelHeight - availableHeight;

		if (overflow <= 0) {
			summaryPanel.style.top = `${SUMMARY_HEADER_GAP}px`;
			return;
		}

		const scrolledPast = SUMMARY_HEADER_GAP - containerRect.top;
		const offset = Math.min(Math.max(scrolledPast, 0), overflow);
		summaryPanel.style.top = `${SUMMARY_HEADER_GAP - offset}px`;
	};

	window.addEventListener("scroll", updateSummarySticky, { passive: true });
	window.addEventListener("resize", updateSummarySticky);

	const renderSummary = () => {
		if (!summaryList) {
			return;
		}
		summaryList.innerHTML = "";

		categories.forEach((category) => {
			if (category.name === RAM_CATEGORY) {
				const ramList = selected.RAM || [];
				if (ramList.length === 0) {
					const li = document.createElement("li");
					const label = document.createElement("span");
					label.textContent = category.label;
					const value = document.createElement("span");
					value.textContent = "미선택";
					value.classList.add("is-unselected");
					li.append(label, value);
					summaryList.appendChild(li);
					return;
				}
				ramList.forEach((item, index) => {
					summaryList.appendChild(renderRamSummaryLine(item, index === 0, category.label));
				});
				return;
			}

			const item = selected[category.name];
			const li = document.createElement("li");
			const label = document.createElement("span");
			label.textContent = category.label;
			const value = document.createElement("span");
			value.textContent = item ? item.name : "미선택";
			value.classList.toggle("is-unselected", !item);
			li.append(label, value);
			summaryList.appendChild(li);
		});

		const singleSelectItems = categories
			.filter((category) => category.name !== RAM_CATEGORY)
			.map((category) => selected[category.name])
			.filter(Boolean);
		const ramItems = selected.RAM || [];

		const partsTotal = singleSelectItems.reduce((sum, item) => sum + Number(item.price), 0)
			+ ramItems.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0);

		const checkedAssembly = document.querySelector("[data-assembly-option]:checked");
		const total = partsTotal + assemblyFee();

		let power = 0;
		let hasPowerInfo = false;
		if (selected.CPU?.powerConsumption != null) {
			power += Number(selected.CPU.powerConsumption);
			hasPowerInfo = true;
		}
		if (selected.GPU?.recommendedPower != null) {
			power += Number(selected.GPU.recommendedPower);
			hasPowerInfo = true;
		}
		if (estimatedPowerElement) {
			estimatedPowerElement.textContent = hasPowerInfo ? `${power} W` : "-";
		}

		if (totalPriceElement) {
			totalPriceElement.textContent = checkedAssembly
				? formatPrice(total)
				: `${formatPrice(partsTotal)} + 조립비`;
		}

		const allSelected = categories.every((category) => isCategorySelected(category.name));
		const issues = evaluateCompatibility();
		renderCompatibilityBanner(issues, allSelected);

		if (submitButton) {
			if (!allSelected) {
				submitButton.disabled = true;
				submitButton.textContent = "부품을 모두 선택해 주세요";
			} else if (!checkedAssembly) {
				submitButton.disabled = true;
				submitButton.textContent = "조립 방식을 선택해 주세요";
			} else if (issues.length > 0) {
				submitButton.disabled = true;
				submitButton.textContent = "호환성 문제를 해결해 주세요";
			} else {
				submitButton.disabled = false;
				submitButton.textContent = "장바구니 담기";
			}
		}

		updateSummarySticky();
	};

	const submitQuote = async () => {
		const checkedAssembly = document.querySelector("[data-assembly-option]:checked");
		if (!checkedAssembly || submitButton?.disabled) {
			return;
		}

		submitButton.disabled = true;
		try {
			const items = [];
			categories.forEach((category) => {
				if (category.name === RAM_CATEGORY) {
					(selected.RAM || []).forEach((ram) => {
						items.push({ productId: ram.productId, quantity: ram.quantity });
					});
					return;
				}
				const item = selected[category.name];
				if (item) {
					items.push({ productId: item.productId, quantity: 1 });
				}
			});

			const response = await fetch("/api/cart/quotes", {
				method: "POST",
				headers: csrfHeaders(),
				body: JSON.stringify({
					items,
					assemblyType: checkedAssembly.value
				})
			});

			if (response.status === 401 || response.redirected) {
				window.location.assign("/login");
				return;
			}

			const result = await response.json().catch(() => ({}));
			if (!response.ok) {
				throw new Error(result.message || result.detail || "장바구니에 담지 못했습니다.");
			}

			showFeedback("장바구니에 견적을 담았습니다.");
			if (window.confirm("상품이 장바구니에 담겼습니다.\n장바구니로 이동하시겠습니까?")) {
				window.location.assign("/cart");
			}
		} catch (error) {
			showFeedback(error.message, true);
		} finally {
			renderSummary();
		}
	};

	categoryButtons.forEach((button) => {
		button.addEventListener("click", () => {
			categoryButtons.forEach((b) => b.classList.toggle("is-active", b === button));
			currentCategory = button.dataset.category;
			if (categoryTitle) {
				categoryTitle.textContent = `${button.textContent.trim()} 선택`;
			}
			brands = [];
			minPrice = null;
			maxPrice = null;
			specFilters = {};
			loadFacets(currentCategory);
			fetchCategoryProducts();
		});
	});

	sortButtons.forEach((button) => {
		button.addEventListener("click", () => {
			sortButtons.forEach((b) => b.classList.toggle("is-active", b === button));
			sort = button.dataset.sort;
			fetchCategoryProducts();
		});
	});

	storageTypeButtons.forEach((button) => {
		button.addEventListener("click", () => {
			storageTypeButtons.forEach((b) => b.classList.toggle("is-active", b === button));
			storageType = button.dataset.storageType;
			fetchCategoryProducts();
		});
	});

	const headerSearchForm = document.querySelector(".header-search");
	const headerSearchInput = document.querySelector("#header-keyword");
	headerSearchForm?.addEventListener("submit", (event) => {
		event.preventDefault();
		keyword = headerSearchInput?.value.trim() ?? "";
		fetchCategoryProducts();
	});

	productGrid?.addEventListener("click", (event) => {
		if (event.target.closest("button")) {
			return;
		}
		const item = event.target.closest(".product-grid-item");
		const productId = item ? Number(item.dataset.productId) : null;
		const product = productId ? productsById.get(productId) : null;
		if (!product) {
			return;
		}
		const isRam = product.category === RAM_CATEGORY;
		window.ProductModal?.open(productId, [
			{
				label: isRam ? "견적에 추가" : "견적에 담기",
				onClick: () => (isRam ? addRamItem(product) : selectItem(product))
			}
		]);
	});

	assemblyOptions.forEach((option) => {
		option.addEventListener("change", renderSummary);
	});

	submitButton?.addEventListener("click", submitQuote);

	loadInitialItems();
	renderSummary();
	loadFacets(currentCategory);
	fetchCategoryProducts();
});
