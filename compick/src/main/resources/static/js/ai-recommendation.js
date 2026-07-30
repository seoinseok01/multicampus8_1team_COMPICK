document.addEventListener("DOMContentLoaded", () => {
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

    document.querySelectorAll(".integration-button").forEach((button) => {
        button.addEventListener("click", () => {
            const notice = document.querySelector(".integration-notice");
            if (notice) notice.hidden = false;
        });
    });
});
