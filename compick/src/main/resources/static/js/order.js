const paymentButton = document.querySelector("#payment-button");
const toast = document.querySelector("#order-toast");
const pageData = document.body.dataset;

function showToast(message) {
	toast.textContent = message;
	toast.classList.add("show");
	window.setTimeout(() => toast.classList.remove("show"), 2400);
}

document.querySelectorAll("[data-toast]").forEach((button) => {
	button.addEventListener("click", () => showToast(button.dataset.toast));
});

function initializeTossPayments() {
	if (!paymentButton || !pageData.tossClientKey) {
		return;
	}

	if (typeof TossPayments !== "function") {
		showToast("토스페이먼츠 SDK를 불러오지 못했습니다.");
		return;
	}

	try {
		const tossPayments = TossPayments(pageData.tossClientKey);
		const payment = tossPayments.payment({
			customerKey: pageData.customerKey
		});
		const amount = {
			currency: "KRW",
			value: Number(pageData.totalAmount)
		};

		paymentButton.disabled = false;
		paymentButton.setAttribute("aria-disabled", "false");
		paymentButton.addEventListener("click", async () => {
			paymentButton.disabled = true;

			try {
				await payment.requestPayment({
					method: "CARD",
					amount,
					orderId: pageData.orderId,
					orderName: pageData.orderName,
					successUrl: `${window.location.origin}/payments/success`,
					failUrl: `${window.location.origin}/payments/fail`,
					customerName: pageData.customerName,
					card: {
						flowMode: "DEFAULT"
					}
				});
			} catch (error) {
				if (error.code !== "USER_CANCEL") {
					showToast(error.message || "결제 요청을 시작하지 못했습니다.");
				}
				paymentButton.disabled = false;
			}
		});
	} catch (error) {
		showToast(error.message || "토스 결제창 초기화에 실패했습니다.");
	}
}

initializeTossPayments();
