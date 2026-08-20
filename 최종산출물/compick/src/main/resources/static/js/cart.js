document.addEventListener("DOMContentLoaded", () => {
	const productList = document.querySelector("[data-product-list]");
	if (!productList) {
		return;
	}

	const quoteList = document.querySelector("[data-quote-list]");
	const productEmpty = document.querySelector("[data-product-empty]");
	const quoteEmpty = document.querySelector("[data-quote-empty]");
	const selectAllProducts = document.querySelector("[data-select-all-products]");
	const selectAllQuotes = document.querySelector("[data-select-all-quotes]");
	const selectedAmountElement = document.querySelector("[data-selected-amount]");
	const totalAmountElement = document.querySelector("[data-total-amount]");
	const checkoutButton = document.querySelector("[data-checkout-button]");
	const loadingIndicator = document.querySelector("[data-loading-indicator]");
	const feedback = document.querySelector("#cart-feedback");

	let state = { products: [], quotes: [] };
	let feedbackTimer = null;

	const formatPrice = (price) => new Intl.NumberFormat("ko-KR").format(Number(price)) + "원";

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

	const request = async (url, options) => {
		const response = await fetch(url, {
			headers: csrfHeaders(),
			...options
		});
		if (response.status === 401 || response.redirected) {
			window.location.assign("/login");
			return null;
		}
		if (!response.ok) {
			const result = await response.json().catch(() => ({}));
			throw new Error(result.message || result.detail || "요청을 처리하지 못했습니다.");
		}
		return response.status === 204 || response.status === 200 && response.headers.get("content-length") === "0"
			? null
			: response.json().catch(() => null);
	};

	const loadCart = async () => {
		if (loadingIndicator) {
			loadingIndicator.hidden = false;
		}
		try {
			const cart = await request("/api/cart");
			if (!cart) {
				return;
			}
			state = cart;
			render();
		} catch (error) {
			showFeedback(error.message, true);
		} finally {
			if (loadingIndicator) {
				loadingIndicator.hidden = true;
			}
		}
	};

	const renderProductLine = (item) => {
		const li = document.createElement("li");
		li.className = "cart-line-item";
		li.classList.toggle("is-unselected", !item.selected);

		const checkbox = document.createElement("input");
		checkbox.type = "checkbox";
		checkbox.checked = item.selected;
		checkbox.addEventListener("change", () => toggleProductSelected(item.productId, checkbox.checked));

		const image = document.createElement("img");
		image.className = "cart-line-thumb";
		image.loading = "lazy";
		image.alt = `${item.name} 상품 이미지`;
		image.src = item.imageUrl && item.imageUrl.length > 0
			? item.imageUrl
			: "/images/products/product-placeholder.svg";
		image.addEventListener("error", () => {
			image.src = "/images/products/product-placeholder.svg";
		}, { once: true });

		const info = document.createElement("div");
		info.className = "cart-line-info";
		const name = document.createElement("h3");
		name.textContent = item.name;
		const meta = document.createElement("p");
		meta.textContent = `${item.category} · ${item.brand}`;
		if (!item.purchasable) {
			const warn = document.createElement("span");
			warn.className = "is-sold-out";
			warn.textContent = " · 재고 부족/판매 중지";
			meta.appendChild(warn);
		}
		info.append(name, meta);

		const stepper = document.createElement("div");
		stepper.className = "cart-stepper";
		const decreaseButton = document.createElement("button");
		decreaseButton.type = "button";
		decreaseButton.textContent = "−";
		decreaseButton.disabled = item.quantity <= 1;
		decreaseButton.addEventListener("click", () => changeProductQuantity(item.productId, item.quantity - 1));
		const quantitySpan = document.createElement("span");
		quantitySpan.textContent = String(item.quantity);
		const increaseButton = document.createElement("button");
		increaseButton.type = "button";
		increaseButton.textContent = "+";
		increaseButton.addEventListener("click", () => changeProductQuantity(item.productId, item.quantity + 1));
		stepper.append(decreaseButton, quantitySpan, increaseButton);

		const price = document.createElement("strong");
		price.className = "cart-line-price";
		price.textContent = formatPrice(item.lineTotal);

		const deleteButton = document.createElement("button");
		deleteButton.type = "button";
		deleteButton.className = "cart-line-delete";
		deleteButton.textContent = "삭제";
		deleteButton.addEventListener("click", () => deleteProduct(item.productId));

		li.append(checkbox, image, info, stepper, price, deleteButton);
		return li;
	};

	const renderQuoteLine = (item) => {
		const li = document.createElement("li");

		const card = document.createElement("div");
		card.className = "cart-quote-card";
		card.classList.toggle("is-unselected", !item.selected);

		const checkbox = document.createElement("input");
		checkbox.type = "checkbox";
		checkbox.checked = item.selected;
		checkbox.addEventListener("change", () => toggleQuoteSelected(item.quoteId, checkbox.checked));

		const body = document.createElement("div");
		body.className = "cart-quote-body";

		const name = document.createElement("h3");
		name.textContent = item.quoteName;

		const meta = document.createElement("p");
		meta.className = `cart-quote-meta ${item.compatible ? "is-pass" : "is-fail"}`;
		meta.textContent = item.compatible
			? `${item.itemCount}개 부품 · 호환성 검사 통과`
			: `${item.itemCount}개 부품 · ⚠ 호환성 문제 ${item.compatibilityIssueCount}건`;

		const itemsList = document.createElement("ul");
		itemsList.className = "cart-quote-items";
		item.items.forEach((preview) => {
			const previewItem = document.createElement("li");
			previewItem.textContent = `${preview.category} ${preview.name}`;
			itemsList.appendChild(previewItem);
		});

		const footer = document.createElement("div");
		footer.className = "cart-quote-footer";

		const editLink = document.createElement("a");
		editLink.className = "cart-quote-edit-link";
		editLink.href = `/quotes/new?basedOn=${item.quoteId}`;
		editLink.textContent = "견적 수정";

		const priceRow = document.createElement("div");
		priceRow.className = "cart-quote-price-row";
		const price = document.createElement("strong");
		price.textContent = formatPrice(item.totalPrice);
		const deleteButton = document.createElement("button");
		deleteButton.type = "button";
		deleteButton.className = "cart-quote-delete";
		deleteButton.textContent = "전체 삭제";
		deleteButton.addEventListener("click", () => deleteQuote(item.quoteId));
		priceRow.append(price, deleteButton);

		footer.append(editLink, priceRow);
		body.append(name, meta, itemsList, footer);
		card.append(checkbox, body);
		li.appendChild(card);
		return li;
	};

	const render = () => {
		productList.innerHTML = "";
		const productFragment = document.createDocumentFragment();
		state.products.forEach((item) => productFragment.appendChild(renderProductLine(item)));
		productList.appendChild(productFragment);
		if (productEmpty) {
			productEmpty.hidden = state.products.length > 0;
		}
		if (selectAllProducts) {
			selectAllProducts.checked = state.products.length > 0 && state.products.every((item) => item.selected);
		}

		if (quoteList) {
			quoteList.innerHTML = "";
			const quoteFragment = document.createDocumentFragment();
			state.quotes.forEach((item) => quoteFragment.appendChild(renderQuoteLine(item)));
			quoteList.appendChild(quoteFragment);
		}
		if (quoteEmpty) {
			quoteEmpty.hidden = state.quotes.length > 0;
		}
		if (selectAllQuotes) {
			selectAllQuotes.checked = state.quotes.length > 0 && state.quotes.every((item) => item.selected);
		}

		renderSummary();
	};

	const renderSummary = () => {
		const selectedProductsTotal = state.products
			.filter((item) => item.selected)
			.reduce((sum, item) => sum + Number(item.lineTotal), 0);
		const selectedQuotesTotal = state.quotes
			.filter((item) => item.selected)
			.reduce((sum, item) => sum + Number(item.totalPrice), 0);
		const total = selectedProductsTotal + selectedQuotesTotal;

		if (selectedAmountElement) {
			selectedAmountElement.textContent = formatPrice(total);
		}
		if (totalAmountElement) {
			totalAmountElement.textContent = formatPrice(total);
		}
		if (checkoutButton) {
			checkoutButton.disabled = total <= 0;
		}
	};

	const toggleProductSelected = async (productId, selected) => {
		try {
			await request(`/api/cart/items/${productId}/selection`, {
				method: "PATCH",
				body: JSON.stringify({ selected })
			});
			await loadCart();
		} catch (error) {
			showFeedback(error.message, true);
			await loadCart();
		}
	};

	const changeProductQuantity = async (productId, quantity) => {
		if (quantity < 1) {
			return;
		}
		try {
			await request(`/api/cart/items/${productId}/quantity`, {
				method: "PATCH",
				body: JSON.stringify({ quantity })
			});
			await loadCart();
		} catch (error) {
			showFeedback(error.message, true);
			await loadCart();
		}
	};

	const deleteProduct = async (productId) => {
		try {
			await request(`/api/cart/items/${productId}`, { method: "DELETE" });
			showFeedback("상품을 삭제했습니다.");
			await loadCart();
		} catch (error) {
			showFeedback(error.message, true);
		}
	};

	const toggleQuoteSelected = async (quoteId, selected) => {
		try {
			await request(`/api/cart/quotes/${quoteId}/selection`, {
				method: "PATCH",
				body: JSON.stringify({ selected })
			});
			await loadCart();
		} catch (error) {
			showFeedback(error.message, true);
			await loadCart();
		}
	};

	const deleteQuote = async (quoteId) => {
		try {
			await request(`/api/cart/quotes/${quoteId}`, { method: "DELETE" });
			showFeedback("견적을 삭제했습니다.");
			await loadCart();
		} catch (error) {
			showFeedback(error.message, true);
		}
	};

	selectAllProducts?.addEventListener("change", async () => {
		const targetSelected = selectAllProducts.checked;
		try {
			await Promise.all(
				state.products
					.filter((item) => item.selected !== targetSelected)
					.map((item) =>
						request(`/api/cart/items/${item.productId}/selection`, {
							method: "PATCH",
							body: JSON.stringify({ selected: targetSelected })
						})
					)
			);
			await loadCart();
		} catch (error) {
			showFeedback(error.message, true);
			await loadCart();
		}
	});

	selectAllQuotes?.addEventListener("change", async () => {
		const targetSelected = selectAllQuotes.checked;
		try {
			await Promise.all(
				state.quotes
					.filter((item) => item.selected !== targetSelected)
					.map((item) =>
						request(`/api/cart/quotes/${item.quoteId}/selection`, {
							method: "PATCH",
							body: JSON.stringify({ selected: targetSelected })
						})
					)
			);
			await loadCart();
		} catch (error) {
			showFeedback(error.message, true);
			await loadCart();
		}
	});

	checkoutButton?.addEventListener("click", () => {
		if (checkoutButton.disabled) {
			return;
		}
		window.location.assign("/orders/new");
	});

	loadCart();
});
