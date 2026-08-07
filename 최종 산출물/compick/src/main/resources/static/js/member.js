document.addEventListener("DOMContentLoaded", () => {
	const loginIdInput = document.querySelector("#loginId");
	const emailInput = document.querySelector("#email");
	const loginIdResult = document.querySelector("[data-login-id-result]");
	const emailResult = document.querySelector("[data-email-result]");

	async function checkDuplicate(path, parameter, value, output) {
		if (!value.trim()) {
			output.textContent = "값을 먼저 입력해 주세요.";
			output.className = "field-result error";
			return;
		}

		try {
			const response = await fetch(
				`${path}?${parameter}=${encodeURIComponent(value.trim())}`
			);
			if (!response.ok) {
				throw new Error("중복 확인 요청에 실패했습니다.");
			}
			const result = await response.json();
			output.textContent = result.available
				? "사용할 수 있습니다."
				: "이미 사용 중입니다.";
			output.className = `field-result ${result.available ? "success" : "error"}`;
		} catch (error) {
			output.textContent = error.message;
			output.className = "field-result error";
		}
	}

	document.querySelector("[data-check-login-id]")?.addEventListener("click", () => {
		checkDuplicate(
			"/api/members/check-login-id",
			"loginId",
			loginIdInput.value,
			loginIdResult
		);
	});

	document.querySelector("[data-check-email]")?.addEventListener("click", () => {
		checkDuplicate(
			"/api/members/check-email",
			"email",
			emailInput.value,
			emailResult
		);
	});
});
