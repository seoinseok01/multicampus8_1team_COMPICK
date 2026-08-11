document.addEventListener("DOMContentLoaded", () => {
	const addButton = document.querySelector("[data-add-preset-to-cart]");
	if (!addButton) {
		return;
	}

	const feedback = document.querySelector("#cart-feedback");
	const quoteId = document.querySelector('meta[name="quote-id"]')?.content;
	let feedbackTimer = null;

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

	addButton.addEventListener("click", async () => {
		if (!quoteId || addButton.disabled) {
			return;
		}
		addButton.disabled = true;

		try {
			const response = await fetch(`/api/cart/quotes/${quoteId}`, {
				method: "POST",
				headers: csrfHeaders()
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
			addButton.disabled = false;
		}
	});
});
