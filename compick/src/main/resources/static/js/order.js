const agreement = document.querySelector("#payment-agreement");
const paymentButton = document.querySelector("#payment-button");
const paymentMethods = document.querySelectorAll(".payment-method");
const toast = document.querySelector("#order-toast");

agreement?.addEventListener("change", () => {
	paymentButton.disabled = !agreement.checked;
});

paymentMethods.forEach((method) => {
	method.addEventListener("click", () => {
		paymentMethods.forEach((item) => item.classList.remove("selected"));
		method.classList.add("selected");
	});
});

function showToast(message) {
	toast.textContent = message;
	toast.classList.add("show");
	window.setTimeout(() => toast.classList.remove("show"), 2400);
}

document.querySelectorAll("[data-toast]").forEach((button) => {
	button.addEventListener("click", () => showToast(button.dataset.toast));
});

paymentButton?.addEventListener("click", () => {
	showToast("결제 기능은 다음 단계에서 연동됩니다.");
});
