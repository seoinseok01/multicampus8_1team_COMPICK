document.addEventListener("DOMContentLoaded", () => {
	const cancelButton = document.querySelector("[data-cancel-button]");
	const returnButton = document.querySelector("[data-return-button]");
	if (!cancelButton && !returnButton) {
		return;
	}

	const orderNumber = document.querySelector('meta[name="order-number"]')?.content;
	const feedback = document.querySelector("#cart-feedback");
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
		const headers = { Accept: "application/json" };
		if (csrfToken && csrfHeader) {
			headers[csrfHeader] = csrfToken;
		}
		return headers;
	};

	const requestAction = async (button, path, confirmMessage, successMessage) => {
		if (!window.confirm(confirmMessage)) {
			return;
		}
		button.disabled = true;
		try {
			const response = await fetch(`/api/orders/${orderNumber}/${path}`, {
				method: "POST",
				headers: csrfHeaders()
			});
			if (response.status === 401 || response.redirected) {
				window.location.assign("/login");
				return;
			}
			if (!response.ok) {
				const result = await response.json().catch(() => ({}));
				throw new Error(result.message || result.detail || "요청을 처리하지 못했습니다.");
			}
			showFeedback(successMessage);
			window.setTimeout(() => window.location.reload(), 800);
		} catch (error) {
			showFeedback(error.message, true);
			button.disabled = false;
		}
	};

	cancelButton?.addEventListener("click", () => {
		requestAction(
			cancelButton,
			"cancel-request",
			"주문을 취소하시겠습니까?",
			"주문 취소를 접수했습니다."
		);
	});

	returnButton?.addEventListener("click", () => {
		requestAction(
			returnButton,
			"return-request",
			"반품을 요청하시겠습니까?",
			"반품 요청이 접수되었습니다."
		);
	});
});
