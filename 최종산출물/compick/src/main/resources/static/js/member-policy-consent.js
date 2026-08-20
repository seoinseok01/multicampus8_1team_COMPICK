document.querySelectorAll("[data-policy-link]").forEach((link) => {
	link.addEventListener("click", (event) => {
		event.preventDefault();
		const policy = link.dataset.policyLink;
		window.open(link.href, `compick-${policy}`, "width=760,height=820,scrollbars=yes");
	});
});

window.addEventListener("message", (event) => {
	if (event.origin !== window.location.origin || event.data?.type !== "compick-policy-agreed") {
		return;
	}
	const checkbox = document.querySelector(`[data-policy-checkbox="${event.data.policy}"]`);
	if (checkbox) {
		checkbox.checked = true;
		checkbox.dispatchEvent(new Event("change", { bubbles: true }));
		checkbox.focus();
	}
});
