document.querySelector("[data-policy-agree]")?.addEventListener("click", (event) => {
	const policy = event.currentTarget.dataset.policyAgree;
	if (window.opener && !window.opener.closed) {
		window.opener.postMessage({ type: "compick-policy-agreed", policy }, window.location.origin);
		window.close();
		return;
	}

	window.history.length > 1 ? window.history.back() : window.location.assign("/members/signup");
});
