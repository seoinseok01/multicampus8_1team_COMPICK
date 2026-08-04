document.addEventListener("DOMContentLoaded", () => {
	const token = document.querySelector('meta[name="_csrf"]')?.content;
	const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

	function headers() {
		const result = {"Content-Type": "application/json"};
		if (token && csrfHeader) {
			result[csrfHeader] = token;
		}
		return result;
	}

	async function request(url, method) {
		const response = await fetch(url, { method, headers: headers() });
		if (!response.ok) {
			let detail = "요청을 처리하지 못했습니다.";
			try {
				const error = await response.json();
				detail = error.detail || error.message || detail;
			} catch (_) {
				// JSON 형식이 아닌 오류 응답은 기본 안내 문구를 사용한다.
			}
			throw new Error(detail);
		}
	}

	document.querySelectorAll("[data-address-delete]").forEach(button => {
		button.addEventListener("click", async () => {
			if (!window.confirm("이 배송지를 삭제하시겠습니까?")) {
				return;
			}
			try {
				await request(
					`/api/addresses/${button.closest("[data-address-id]").dataset.addressId}`,
					"DELETE"
				);
				window.location.reload();
			} catch (error) {
				window.alert(error.message);
			}
		});
	});

	document.querySelectorAll("[data-address-default]").forEach(button => {
		button.addEventListener("click", async () => {
			try {
				await request(
					`/api/addresses/${button.closest("[data-address-id]").dataset.addressId}/default`,
					"PATCH"
				);
				window.location.reload();
			} catch (error) {
				window.alert(error.message);
			}
		});
	});
});
