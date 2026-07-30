document.addEventListener("DOMContentLoaded", () => {
	const form = document.querySelector("#address-form");
	if (!form) {
		return;
	}

	const token = document.querySelector('meta[name="_csrf"]')?.content;
	const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
	const message = document.querySelector("#address-message");
	const addressId = document.querySelector("#addressId");

	function headers() {
		const result = {"Content-Type": "application/json"};
		if (token && csrfHeader) {
			result[csrfHeader] = token;
		}
		return result;
	}

	function payload() {
		return {
			addressName: document.querySelector("#addressName").value,
			recipientName: document.querySelector("#recipientName").value,
			phone: document.querySelector("#addressPhone").value,
			zipCode: document.querySelector("#zipCode").value,
			address1: document.querySelector("#address1").value,
			address2: document.querySelector("#address2").value,
			isDefault: document.querySelector("#isDefault").checked
		};
	}

	async function request(url, method, body) {
		const response = await fetch(url, {
			method,
			headers: headers(),
			body: body ? JSON.stringify(body) : undefined
		});
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

	form.addEventListener("submit", async event => {
		event.preventDefault();
		const id = addressId.value;
		try {
			await request(
				id ? `/api/addresses/${id}` : "/api/addresses",
				id ? "PUT" : "POST",
				payload()
			);
			window.location.reload();
		} catch (error) {
			message.textContent = error.message;
			message.className = "form-message error";
		}
	});

	form.addEventListener("reset", () => {
		addressId.value = "";
		document.querySelector("#address-form-title").textContent = "배송지 등록";
		message.textContent = "";
	});

	document.querySelectorAll("[data-address-edit]").forEach(button => {
		button.addEventListener("click", () => {
			const item = button.closest("[data-address-id]");
			addressId.value = item.dataset.addressId;
			document.querySelector("#addressName").value = item.dataset.addressName;
			document.querySelector("#recipientName").value = item.dataset.recipientName;
			document.querySelector("#addressPhone").value = item.dataset.phone;
			document.querySelector("#zipCode").value = item.dataset.zipCode;
			document.querySelector("#address1").value = item.dataset.address1;
			document.querySelector("#address2").value = item.dataset.address2 || "";
			document.querySelector("#isDefault").checked = item.dataset.default === "true";
			document.querySelector("#address-form-title").textContent = "배송지 수정";
			form.scrollIntoView({behavior: "smooth"});
		});
	});

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
