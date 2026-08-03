document.addEventListener("DOMContentLoaded", () => {
	const loginIdInput = document.querySelector("#loginId");
	const emailInput = document.querySelector("#email");
	const loginIdResult = document.querySelector("[data-login-id-result]");
	const emailResult = document.querySelector("[data-email-result]");
	const verificationCodeInput = document.querySelector("#verificationCode");
	const verificationResult = document.querySelector("[data-verification-result]");

	function csrfHeaders() {
		const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
		const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
		const headers = { "Content-Type": "application/json" };
		if (csrfToken && csrfHeader) {
			headers[csrfHeader] = csrfToken;
		}
		return headers;
	}

	async function requestVerification(url, body, output, successMessage) {
		if (!emailInput.value.trim()) {
			output.textContent = "이메일을 먼저 입력해 주세요.";
			output.className = "field-result error";
			return;
		}

		try {
			const response = await fetch(url, {
				method: "POST",
				headers: csrfHeaders(),
				body: JSON.stringify(body)
			});
			const result = await response.json().catch(() => ({}));
			if (!response.ok) {
				throw new Error(result.message || result.detail || "요청을 처리하지 못했습니다.");
			}
			output.textContent = result.message || successMessage;
			output.className = "field-result success";
		} catch (error) {
			output.textContent = error.message;
			output.className = "field-result error";
		}
	}

	document.querySelector("[data-send-verification]")?.addEventListener("click", () => {
		requestVerification(
			"/api/email-verifications/send",
			{ email: emailInput.value.trim(), purpose: "SIGN_UP" },
			verificationResult,
			"인증번호를 발송했습니다."
		);
	});

	document.querySelector("[data-confirm-verification]")?.addEventListener("click", () => {
		requestVerification(
			"/api/email-verifications/confirm",
			{
				email: emailInput.value.trim(),
				purpose: "SIGN_UP",
				code: verificationCodeInput.value.trim()
			},
			verificationResult,
			"이메일 인증이 완료되었습니다."
		);
	});

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
