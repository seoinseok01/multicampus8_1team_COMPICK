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
<<<<<<< HEAD
				window.location.assign("/members/login");
=======
				window.location.assign("/login");
>>>>>>> 48ad55d3c2f8342386c89a8e9f5dff696b5a09ad
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
