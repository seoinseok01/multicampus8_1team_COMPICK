document.addEventListener("DOMContentLoaded", () => {
	const dialog = document.querySelector("#product-detail-dialog");
	const closeButton = dialog?.querySelector(".dialog-close-button");
	const dialogCartButton = dialog?.querySelector(".dialog-add-cart-button");
	const dialogQuoteLink = dialog?.querySelector(".dialog-add-quote-link");
	const feedback = document.querySelector("#cart-feedback");
	let selectedProductId = null;
	let feedbackTimer = null;

	document.querySelectorAll("img[data-fallback-src]").forEach((image) => {
		image.addEventListener("error", () => {
			const fallbackSource = image.dataset.fallbackSrc;
			if (!fallbackSource || image.getAttribute("src") === fallbackSource) {
				return;
			}

			image.src = fallbackSource;
			image.classList.add("is-fallback");
		}, { once: true });
	});

	const formatPrice = (price) =>
		new Intl.NumberFormat("ko-KR").format(Number(price)) + "원";

	const popularTabs = document.querySelectorAll("[data-popular-tab]");
	const popularPanels = document.querySelectorAll("[data-popular-panel]");
	popularTabs.forEach((tab) => {
		tab.addEventListener("click", () => {
			popularTabs.forEach((item) => item.classList.toggle("is-active", item === tab));
			popularPanels.forEach((panel) => {
				panel.hidden = panel.dataset.popularPanel !== tab.dataset.popularTab;
			});
		});
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

	const setText = (selector, value) => {
		const element = dialog?.querySelector(selector);
		if (element) {
			element.textContent = value ?? "";
		}
	};

	const openProductDialog = (trigger) => {
		if (!dialog) {
			return;
		}

		selectedProductId = Number(trigger.dataset.productId);
		const stockQuantity = Number(trigger.dataset.productStock);

		setText("[data-dialog-category]", trigger.dataset.productCategory);
		setText("[data-dialog-brand]", trigger.dataset.productBrand);
		setText("[data-dialog-name]", trigger.dataset.productName);
		setText("[data-dialog-description]", trigger.dataset.productDescription);
		setText("[data-dialog-detail]", trigger.dataset.productDescription);
		setText("[data-dialog-price]", formatPrice(trigger.dataset.productPrice));
		setText("[data-dialog-stock]", stockQuantity > 0 ? "재고 있음" : "품절");

		for (let index = 1; index <= 4; index += 1) {
			setText(
				`[data-dialog-spec-label="${index}"]`,
				trigger.dataset[`specLabel${index}`]
			);
			setText(
				`[data-dialog-spec-value="${index}"]`,
				trigger.dataset[`specValue${index}`]
			);
		}

		const stockStatus = dialog.querySelector("[data-dialog-stock]");
		stockStatus?.classList.toggle("is-sold-out", stockQuantity <= 0);
		if (dialogCartButton) {
			dialogCartButton.disabled = stockQuantity <= 0;
		}
		if (dialogQuoteLink) {
			dialogQuoteLink.href = `/quotes/new?productId=${selectedProductId}`;
		}

		dialog.showModal();
	};

	const addToCart = async (productId, button) => {
		if (!productId || button?.disabled) {
			return;
		}

		const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
		const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
		const headers = {
			"Content-Type": "application/json",
			Accept: "application/json"
		};

		if (csrfToken && csrfHeader) {
			headers[csrfHeader] = csrfToken;
		}

		if (button) {
			button.disabled = true;
		}

		try {
			const response = await fetch("/api/cart/items", {
				method: "POST",
				headers,
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
			if (dialog?.open) {
				dialog.close();
			}
		} catch (error) {
			showFeedback(error.message, true);
		} finally {
			if (button) {
				button.disabled = false;
			}
		}
	};

	document.querySelectorAll(".product-detail-trigger").forEach((trigger) => {
		trigger.addEventListener("click", (event) => {
			event.preventDefault();
			openProductDialog(trigger);
		});
	});

	document.querySelectorAll(".product-card .add-cart-button").forEach((button) => {
		button.addEventListener("click", () => {
			addToCart(Number(button.dataset.productId), button);
		});
	});

	const CATEGORY_LABELS = {
		CPU: "CPU",
		CPU_COOLER: "CPU 쿨러",
		MAINBOARD: "메인보드",
		RAM: "RAM",
		GPU: "그래픽카드",
		STORAGE: "저장장치",
		POWER_SUPPLY: "파워",
		CASE: "케이스"
	};

	const highlightDialog = document.querySelector("#ai-highlight-dialog");
	if (highlightDialog) {
		const closeHighlightButton = highlightDialog.querySelector(".dialog-close-button");
		const cta = highlightDialog.querySelector("[data-dialog-cta]");

		const openHighlightDialog = (card) => {
			const requirement = card.dataset.keywords ?? "";
			const explanation = card.dataset.explanation ?? "";
			const price = card.querySelector(".ai-highlight-price")?.textContent ?? "";
			const items = Array.from(card.querySelectorAll(".ai-highlight-items li"));

			highlightDialog.querySelector("[data-dialog-requirement]").textContent = requirement;
			const explanationElement = highlightDialog.querySelector("[data-dialog-explanation]");
			explanationElement.textContent = explanation;
			explanationElement.hidden = explanation === "";
			highlightDialog.querySelector("[data-dialog-price]").textContent = price;

			const itemsList = highlightDialog.querySelector("[data-dialog-items]");
			itemsList.innerHTML = "";
			items.forEach((item) => {
				const row = document.createElement("li");
				const label = document.createElement("span");
				label.textContent = CATEGORY_LABELS[item.dataset.category] ?? item.dataset.category;
				const name = document.createElement("span");
				name.textContent = item.textContent;
				const itemPrice = document.createElement("span");
				itemPrice.textContent = formatPrice(item.dataset.price);
				row.append(label, name, itemPrice);
				itemsList.appendChild(row);
			});

			const params = new URLSearchParams();
			items.forEach((item) => params.append("productId", item.dataset.productId));
			cta.href = `/quotes/new?${params.toString()}`;

			highlightDialog.showModal();
		};

		document.querySelectorAll(".ai-highlight-card").forEach((card) => {
			card.addEventListener("click", () => openHighlightDialog(card));
			card.addEventListener("keydown", (event) => {
				if (event.key === "Enter" || event.key === " ") {
					event.preventDefault();
					openHighlightDialog(card);
				}
			});
		});

		closeHighlightButton?.addEventListener("click", () => highlightDialog.close());
		highlightDialog.addEventListener("click", (event) => {
			const bounds = highlightDialog.getBoundingClientRect();
			const clickedOutside =
				event.clientX < bounds.left ||
				event.clientX > bounds.right ||
				event.clientY < bounds.top ||
				event.clientY > bounds.bottom;

			if (clickedOutside) {
				highlightDialog.close();
			}
		});
	}

	const highlightTrack = document.querySelector(".ai-highlight-track");
	if (highlightTrack) {
		const prevButton = document.querySelector(".ai-highlight-nav.prev");
		const nextButton = document.querySelector(".ai-highlight-nav.next");
		const AUTO_ADVANCE_MS = 4000;
		let autoAdvanceTimer = null;

		const cardScrollStep = () => {
			const card = highlightTrack.querySelector(".ai-highlight-card");
			const gap = parseFloat(getComputedStyle(highlightTrack).columnGap) || 0;
			return card ? card.getBoundingClientRect().width + gap : highlightTrack.clientWidth;
		};

		const scrollByCard = (direction) => {
			const maxScroll = highlightTrack.scrollWidth - highlightTrack.clientWidth;
			const next = highlightTrack.scrollLeft + direction * cardScrollStep();
			highlightTrack.scrollTo({
				left: next < 0 ? maxScroll : next > maxScroll ? 0 : next,
				behavior: "smooth"
			});
		};

		const stopAutoAdvance = () => window.clearInterval(autoAdvanceTimer);
		const startAutoAdvance = () => {
			stopAutoAdvance();
			autoAdvanceTimer = window.setInterval(() => scrollByCard(1), AUTO_ADVANCE_MS);
		};

		prevButton?.addEventListener("click", () => scrollByCard(-1));
		nextButton?.addEventListener("click", () => scrollByCard(1));
		highlightTrack.addEventListener("mouseenter", stopAutoAdvance);
		highlightTrack.addEventListener("mouseleave", startAutoAdvance);

		startAutoAdvance();
	}

	closeButton?.addEventListener("click", () => dialog.close());
	dialogCartButton?.addEventListener("click", () => {
		addToCart(selectedProductId, dialogCartButton);
	});

	dialog?.addEventListener("click", (event) => {
		const bounds = dialog.getBoundingClientRect();
		const clickedOutside =
			event.clientX < bounds.left ||
			event.clientX > bounds.right ||
			event.clientY < bounds.top ||
			event.clientY > bounds.bottom;

		if (clickedOutside) {
			dialog.close();
		}
	});
});
