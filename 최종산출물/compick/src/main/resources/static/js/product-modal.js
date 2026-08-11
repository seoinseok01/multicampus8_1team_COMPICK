window.ProductModal = (function () {
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
		"Efficiency": "인증 등급",
		"Core Clock": "코어 클럭",
		"Boost Clock": "부스트 클럭",
		"Microarchitecture": "아키텍처",
		"TDP": "TDP",
		"Color": "색상",
		"Radiator": "라디에이터",
		"Side Panel": "측면 패널",
		"Max GPU Length": "최대 GPU 길이"
	};

	let backdrop = null;

	const formatPrice = (price) => new Intl.NumberFormat("ko-KR").format(Number(price)) + "원";

	const onKeydown = (event) => {
		if (event.key === "Escape") {
			close();
		}
	};

	function close() {
		if (!backdrop) {
			return;
		}
		backdrop.remove();
		backdrop = null;
		document.removeEventListener("keydown", onKeydown);
	}

	function render(product, actions) {
		close();

		backdrop = document.createElement("div");
		backdrop.className = "product-modal-backdrop";
		backdrop.addEventListener("click", (event) => {
			if (event.target === backdrop) {
				close();
			}
		});

		const modal = document.createElement("div");
		modal.className = "product-modal";

		const closeButton = document.createElement("button");
		closeButton.type = "button";
		closeButton.className = "product-modal-close";
		closeButton.textContent = "✕";
		closeButton.setAttribute("aria-label", "닫기");
		closeButton.addEventListener("click", close);

		const body = document.createElement("div");
		body.className = "product-modal-body";

		const left = document.createElement("div");
		const badge = document.createElement("span");
		badge.className = "product-modal-category";
		badge.textContent = product.categoryLabel;

		const image = document.createElement("img");
		image.className = "product-modal-image";
		image.alt = `${product.name} 상품 이미지`;
		image.src = product.imageUrl && product.imageUrl.length > 0
			? product.imageUrl
			: "/images/products/product-placeholder.svg";
		image.addEventListener("error", () => {
			image.src = "/images/products/product-placeholder.svg";
		}, { once: true });

		const descriptionTitle = document.createElement("h3");
		descriptionTitle.className = "product-modal-description-title";
		descriptionTitle.textContent = "상세 설명";

		const description = document.createElement("p");
		description.className = "product-modal-description";
		description.textContent = product.description && product.description.length > 0
			? product.description
			: "등록된 상세 설명이 없습니다.";

		left.append(badge, image, descriptionTitle, description);

		const right = document.createElement("div");
		const brand = document.createElement("p");
		brand.className = "product-modal-brand";
		brand.textContent = product.brand;

		const name = document.createElement("h2");
		name.className = "product-modal-name";
		name.textContent = product.name;

		const priceRow = document.createElement("div");
		priceRow.className = "product-modal-price-row";
		const price = document.createElement("strong");
		price.className = "product-modal-price";
		price.textContent = formatPrice(product.price);
		const stock = document.createElement("span");
		stock.className = "product-modal-stock" + (product.inStock ? "" : " is-out");
		stock.textContent = product.inStock ? "재고 있음" : "품절";
		priceRow.append(price, stock);

		const specTitle = document.createElement("h3");
		specTitle.className = "product-modal-spec-title";
		specTitle.textContent = "주요 사양";

		const specList = document.createElement("ul");
		specList.className = "product-modal-spec-list";
		Object.entries(product.specs || {}).forEach(([key, value]) => {
			const li = document.createElement("li");
			const k = document.createElement("span");
			k.textContent = SPEC_LABELS[key] ?? key;
			const v = document.createElement("span");
			v.textContent = value;
			li.append(k, v);
			specList.appendChild(li);
		});

		const actionsRow = document.createElement("div");
		actionsRow.className = "product-modal-actions";
		(actions || []).forEach((action) => {
			const button = document.createElement("button");
			button.type = "button";
			button.className = action.className || "primary-button";
			button.textContent = action.label;
			button.disabled = action.disabled ?? !product.inStock;
			button.addEventListener("click", () => {
				action.onClick(product);
				if (action.closeOnClick !== false) {
					close();
				}
			});
			actionsRow.appendChild(button);
		});

		right.append(brand, name, priceRow, specTitle, specList, actionsRow);
		body.append(left, right);
		modal.append(closeButton, body);
		backdrop.appendChild(modal);
		document.body.appendChild(backdrop);
		document.addEventListener("keydown", onKeydown);
	}

	async function open(productId, actions) {
		try {
			const response = await fetch(`/api/products/detail/${productId}`);
			if (!response.ok) {
				return;
			}
			const product = await response.json();
			render(product, actions);
		} catch (error) {
			// 상세 정보를 불러오지 못하면 조용히 무시한다
		}
	}

	return { open, close };
})();
