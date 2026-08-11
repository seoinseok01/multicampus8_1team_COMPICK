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

const CASE_BAY_SPEC_KEY = "Internal 3.5\" Bays";

/* 필터 드롭다운에 나열되는 값은 기본적으로 spec_json 원본 그대로 보여준다(AM4, ATX,
 * GeForce RTX 5090 등은 그대로가 자연스럽다). CPU_COOLER의 "Type"(air/Liquid)만
 * 예외로, 카드와 동일하게 공랭/수냉으로 바꿔서 보여준다. */
const formatFacetValue = (category, specKey, value) => {
	if (category === "CPU_COOLER" && specKey === "Type") {
		const lower = value.toLowerCase();
		return lower.includes("liquid") || lower.includes("water") ? "수냉" : "공랭";
	}
	return value;
};

/* 견적 카드에 보여줄 스펙 요약을 카테고리별로 고른다(전체 spec_json을 그대로 나열하면
 * 줄바꿈 수가 상품마다 달라져 카드 높이가 들쭉날쭉해진다 — CPU_COOLER 자연어 변환, GPU/CASE
 * 순서 지정, MAINBOARD 색상 제외 등 표시 형식도 여기서 함께 정리한다). */
const getDisplaySpecs = (product) => {
	const specs = product.specs ?? {};
	switch (product.category) {
		case "CPU":
			return [
				specs["Socket"],
				specs["Core Count"] ? `${specs["Core Count"]}코어` : null,
				product.powerConsumption != null ? `${product.powerConsumption}W` : null
			].filter(Boolean);
		case "CPU_COOLER": {
			const type = (specs["Type"] ?? "").toLowerCase();
			if (!type) return [];
			return [type.includes("liquid") || type.includes("water") ? "수냉" : "공랭"];
		}
		case "MAINBOARD":
			return Object.entries(specs)
				.filter(([key]) => key !== "Color")
				.map(([, value]) => value)
				.filter(Boolean);
		case "GPU":
			return ["Color", "Core Clock", "Boost Clock", "Chipset", "Length", "Memory"]
				.map((key) => specs[key])
				.filter(Boolean);
		case "CASE":
			return ["Color", "Side Panel", "Max GPU Length", "Form Factor"]
				.map((key) => specs[key])
				.filter(Boolean);
		default:
			return Object.values(specs).filter(Boolean);
	}
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
	const RAM_MODULES_PER_PRODUCT = 2;
	const RAM_CATEGORY = "RAM";
	const MAINBOARD_CATEGORY = "MAINBOARD";
	const STORAGE_CATEGORY = "STORAGE";
	const CASE_CATEGORY = "CASE";
	const HDD_FORM_FACTOR = "3.5\"";
	// RAM과 저장장치(SSD/HDD)는 여러 개를 함께 담을 수 있다. 그 외 카테고리는 1개만 선택한다.
	const MULTI_SELECT_CATEGORIES = new Set([RAM_CATEGORY, STORAGE_CATEGORY]);
	const isMultiSelectCategory = (categoryName) => MULTI_SELECT_CATEGORIES.has(categoryName);
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
					option.textContent = formatFacetValue(category, key, value);
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
		isMultiSelectCategory(categoryName)
			? Boolean(selected[categoryName] && selected[categoryName].length > 0)
			: Boolean(selected[categoryName]);

	const ramSlotLimit = () => selected.MAINBOARD?.memorySlots ?? null;
	const ramProductLimit = () => {
		const slots = ramSlotLimit();
		return slots == null ? null : Math.floor(slots / RAM_MODULES_PER_PRODUCT);
	};

	const loadInitialItems = () => {
		try {
			const items = JSON.parse(window.__QUOTE_INITIAL_ITEMS__ ?? "[]");
			items.forEach((item) => {
				if (isMultiSelectCategory(item.category)) {
					const list = selected[item.category] || (selected[item.category] = []);
					list.push({ ...item, quantity: item.quantity ?? 1 });
				} else {
					selected[item.category] = item;
				}
			});
		} catch (error) {
			// 초기 프리필 데이터가 없거나 잘못돼도 빈 견적으로 시작한다
		}
	};

	const multiTotalQuantity = (category) =>
		(selected[category] || []).reduce((sum, item) => sum + item.quantity, 0);

	// 케이스에 꽂을 수 있는 3.5인치 저장장치(HDD) 개수. 케이스를 아직 안 골랐으면 제한 없음(null).
	const hddBayLimit = () => {
		const bays = parseInt(selected.CASE?.specs?.[CASE_BAY_SPEC_KEY], 10);
		return Number.isNaN(bays) ? null : bays;
	};
	const hddTotalQuantity = () =>
		(selected.STORAGE || [])
			.filter((item) => item.formFactor === HDD_FORM_FACTOR)
			.reduce((sum, item) => sum + item.quantity, 0);

	// RAM은 메인보드 슬롯 수, 3.5인치 HDD는 케이스 베이 수에 따른 상한이 있다.
	const checkMultiSelectLimit = (category, item, additionalQuantity) => {
		if (category === RAM_CATEGORY) {
			const maxProducts = ramProductLimit();
			if (maxProducts != null && multiTotalQuantity(RAM_CATEGORY) + additionalQuantity > maxProducts) {
				window.alert(`이 메인보드는 RAM 상품을 최대 ${maxProducts}개까지 선택할 수 있습니다. (상품 1개당 RAM 2개 구성)`);
				return false;
			}
		}
		if (category === STORAGE_CATEGORY && item.formFactor === HDD_FORM_FACTOR) {
			const bays = hddBayLimit();
			if (bays != null && hddTotalQuantity() + additionalQuantity > bays) {
				window.alert(`선택한 케이스는 3.5인치 저장장치(HDD)를 최대 ${bays}개까지 장착할 수 있습니다.`);
				return false;
			}
		}
		return true;
	};

	const addMultiItem = (category, product) => {
		const list = selected[category] || (selected[category] = []);
		if (!checkMultiSelectLimit(category, product, 1)) {
			return;
		}
		const existing = list.find((item) => item.productId === product.productId);
		if (existing) {
			existing.quantity += 1;
		} else {
			list.push({ ...product, quantity: 1 });
		}
		fetchCategoryProducts();
		renderSummary();
	};

	const changeMultiItemQuantity = (category, productId, delta) => {
		const list = selected[category] || [];
		const item = list.find((entry) => entry.productId === productId);
		if (!item) {
			return;
		}
		if (delta > 0 && !checkMultiSelectLimit(category, item, 1)) {
			return;
		}
		item.quantity += delta;
		if (item.quantity <= 0) {
			selected[category] = list.filter((entry) => entry.productId !== productId);
		}
		if (currentCategory === category) {
			fetchCategoryProducts();
		}
		renderSummary();
	};

	const removeMultiItem = (category, productId) => {
		selected[category] = (selected[category] || []).filter((entry) => entry.productId !== productId);
		if (currentCategory === category) {
			fetchCategoryProducts();
		}
		renderSummary();
	};

	const enforceRamSlotLimit = () => {
		const maxProducts = ramProductLimit();
		if (maxProducts == null || !selected.RAM || selected.RAM.length === 0) {
			return;
		}
		if (multiTotalQuantity(RAM_CATEGORY) > maxProducts) {
			selected.RAM = [];
			window.alert(
				`메인보드 변경으로 RAM 선택이 초기화되었습니다. RAM 상품은 최대 ${maxProducts}개까지 선택할 수 있습니다. (상품 1개당 RAM 2개 구성)`
			);
		}
	};

	// 케이스를 나중에 고르거나 바꿨을 때, 이미 담아둔 3.5인치 HDD가 새 케이스 베이 수를
	// 넘으면 초과분을 비우고 알려준다(RAM의 enforceRamSlotLimit과 동일한 패턴).
	const enforceCaseBayLimit = () => {
		const bays = hddBayLimit();
		if (bays == null || !selected.STORAGE || selected.STORAGE.length === 0) {
			return;
		}
		if (hddTotalQuantity() > bays) {
			selected.STORAGE = selected.STORAGE.filter((item) => item.formFactor !== HDD_FORM_FACTOR);
			window.alert(
				`케이스 변경으로 3.5인치 저장장치(HDD) 선택이 초기화되었습니다. 이 케이스는 3.5인치 저장장치를 최대 ${bays}개까지 지원합니다.`
			);
		}
	};

	const selectItem = (product) => {
		selected[product.category] = product;
		if (product.category === MAINBOARD_CATEGORY) {
			enforceRamSlotLimit();
		}
		if (product.category === CASE_CATEGORY) {
			enforceCaseBayLimit();
		}
		fetchCategoryProducts();
		renderSummary();
	};

	const renderProductCard = (product) => {
		const li = document.createElement("li");
		li.className = "product-grid-item";
		li.dataset.productId = product.productId;

		const isMultiSelect = isMultiSelectCategory(product.category);
		const multiLine = isMultiSelect
			? (selected[product.category] || []).find((item) => item.productId === product.productId)
			: null;
		const isSingleSelected = !isMultiSelect && selected[product.category]?.productId === product.productId;
		if (isSingleSelected || multiLine) {
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
		specLine.textContent = getDisplaySpecs(product).join(" / ");

		const price = document.createElement("strong");
		price.textContent = formatPrice(product.price);

		const button = document.createElement("button");
		button.type = "button";
		if (isMultiSelect) {
			button.textContent = multiLine ? `추가 (담김 ${multiLine.quantity}개)` : "추가";
		} else {
			button.textContent = isSingleSelected ? "선택됨" : "담기";
		}
		button.disabled = product.stockQuantity <= 0;
		button.addEventListener("click", () => {
			if (isMultiSelect) {
				addMultiItem(product.category, product);
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

		// 3.5인치 HDD는 케이스 내부 베이에 물리적으로 장착해야 한다(2.5인치 SSD·M.2는 케이스 베이를
		// 차지하지 않으므로 검사하지 않는다). 담을 때 이미 개수를 막아두지만(checkMultiSelectLimit),
		// 프리필된 견적 등 다른 경로로 들어온 값까지 한 번 더 확인하는 안전망이다.
		if (pcCase) {
			const bays = hddBayLimit();
			const hddCount = hddTotalQuantity();
			if (bays != null && hddCount > bays) {
				issues.push(`3.5인치 HDD를 ${hddCount}개 선택했지만, 케이스 내부 베이는 ${bays}개까지 지원합니다.`);
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
			span.textContent = "소켓·메모리 규격·폼팩터·GPU 길이·파워 용량·저장장치 베이 수를 확인했습니다.";
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

	const renderMultiItemSummaryLine = (category, item, showLabel, label) => {
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
		minusButton.addEventListener("click", () => changeMultiItemQuantity(category, item.productId, -1));

		const plusButton = document.createElement("button");
		plusButton.type = "button";
		plusButton.textContent = "+";
		plusButton.setAttribute("aria-label", `${item.name} 수량 늘리기`);
		plusButton.addEventListener("click", () => changeMultiItemQuantity(category, item.productId, 1));

		const removeButton = document.createElement("button");
		removeButton.type = "button";
		removeButton.className = "ram-remove";
		removeButton.textContent = "✕";
		removeButton.setAttribute("aria-label", `${item.name} 삭제`);
		removeButton.addEventListener("click", () => removeMultiItem(category, item.productId));

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
			if (isMultiSelectCategory(category.name)) {
				const list = selected[category.name] || [];
				if (list.length === 0) {
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
				list.forEach((item, index) => {
					summaryList.appendChild(renderMultiItemSummaryLine(category.name, item, index === 0, category.label));
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
			.filter((category) => !isMultiSelectCategory(category.name))
			.map((category) => selected[category.name])
			.filter(Boolean);
		const multiSelectItems = categories
			.filter((category) => isMultiSelectCategory(category.name))
			.flatMap((category) => selected[category.name] || []);

		const partsTotal = singleSelectItems.reduce((sum, item) => sum + Number(item.price), 0)
			+ multiSelectItems.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0);

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
				if (isMultiSelectCategory(category.name)) {
					(selected[category.name] || []).forEach((entry) => {
						items.push({ productId: entry.productId, quantity: entry.quantity });
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
		const isMultiSelect = isMultiSelectCategory(product.category);
		window.ProductModal?.open(productId, [
			{
				label: isMultiSelect ? "견적에 추가" : "견적에 담기",
				onClick: () => (isMultiSelect ? addMultiItem(product.category, product) : selectItem(product))
			}
		]);
	});

	assemblyOptions.forEach((option) => {
		option.addEventListener("change", renderSummary);
	});

	submitButton?.addEventListener("click", submitQuote);

	loadInitialItems();
	enforceRamSlotLimit();
	renderSummary();
	loadFacets(currentCategory);
	fetchCategoryProducts();
});
