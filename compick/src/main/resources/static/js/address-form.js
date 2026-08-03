document.addEventListener("DOMContentLoaded", () => {
	const form = document.querySelector("#address-form");
	if (!form) {
		return;
	}

	const addressId = document.body.dataset.addressId || null;
	const token = document.querySelector('meta[name="_csrf"]')?.content;
	const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
	const message = document.querySelector("#address-message");

	function headers() {
		const result = {"Content-Type": "application/json"};
		if (token && csrfHeader) {
			result[csrfHeader] = token;
		}
		return result;
	}

	async function loadForEdit() {
		if (!addressId) {
			return;
		}
		const response = await fetch("/api/addresses", { headers: headers() });
		if (!response.ok) {
			return;
		}
		const addresses = await response.json();
		const address = addresses.find(item => String(item.addressId) === addressId);
		if (!address) {
			return;
		}
		document.querySelector("#addressName").value = address.addressName || "";
		document.querySelector("#recipientName").value = address.recipientName;
		document.querySelector("#addressPhone").value = address.phone;
		document.querySelector("#zipCode").value = address.zipCode;
		document.querySelector("#address1").value = address.address1;
		document.querySelector("#address2").value = address.address2 || "";
		document.querySelector("#isDefault").checked = address.isDefault;
	}

	document.querySelector("[data-address-search]")?.addEventListener("click", () => {
		new daum.Postcode({
			oncomplete: data => {
				document.querySelector("#zipCode").value = data.zonecode;
				document.querySelector("#address1").value = data.roadAddress || data.jibunAddress;
				document.querySelector("#address2").focus();
			}
		}).open();
	});

	form.addEventListener("submit", async event => {
		event.preventDefault();
		const payload = {
			addressName: document.querySelector("#addressName").value,
			recipientName: document.querySelector("#recipientName").value,
			phone: document.querySelector("#addressPhone").value,
			zipCode: document.querySelector("#zipCode").value,
			address1: document.querySelector("#address1").value,
			address2: document.querySelector("#address2").value,
			isDefault: document.querySelector("#isDefault").checked
		};

		try {
			const response = await fetch(
				addressId ? `/api/addresses/${addressId}` : "/api/addresses",
				{
					method: addressId ? "PUT" : "POST",
					headers: headers(),
					body: JSON.stringify(payload)
				}
			);
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
			window.location.href = "/mypage/addresses";
		} catch (error) {
			message.textContent = error.message;
			message.className = "form-message error";
		}
	});

	loadForEdit();
});
