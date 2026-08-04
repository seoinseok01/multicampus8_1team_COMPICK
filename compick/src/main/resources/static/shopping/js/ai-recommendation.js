document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("#ai-quote-form");
    const textarea = document.querySelector("#requirements");
    const inputView = document.querySelector(".ai-input-view");
    const loadingView = document.querySelector("#ai-loading-view");
    const loadingRequest = document.querySelector("#loading-request");

    document.querySelectorAll("[data-condition]").forEach((button) => {
        button.addEventListener("click", () => {
            const condition = button.dataset.condition;
            const separator = textarea.value.trim() ? " · " : "";
            if (!textarea.value.includes(condition)) {
                textarea.value += separator + condition;
            }
            textarea.focus();
        });
    });

    form?.addEventListener("submit", () => {
        if (!form.checkValidity() || !textarea.value.trim()) {
            return;
        }
        loadingRequest.textContent = textarea.value.trim();
        inputView.classList.add("is-hidden");
        loadingView.classList.remove("is-hidden");
        form.querySelector("button[type='submit']").disabled = true;
    });

    const addToCartButton = document.querySelector("[data-add-quote-to-cart]");
    addToCartButton?.addEventListener("click", async () => {
        const quoteId = addToCartButton.dataset.quoteId;
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
        const headers = {};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        addToCartButton.disabled = true;
        try {
            const response = await fetch(`/api/cart/quotes/${quoteId}`, {
                method: "POST",
                headers
            });
            const result = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw new Error(result.message || result.detail || "장바구니에 담지 못했습니다.");
            }
            if (window.confirm("견적을 장바구니에 담았습니다.\n장바구니로 이동하시겠습니까?")) {
                window.location.assign("/cart");
            }
        } catch (error) {
            window.alert(error.message);
        } finally {
            addToCartButton.disabled = false;
        }
    });
});
