document.addEventListener("DOMContentLoaded", () => {
	const itemList = document.querySelector("[data-order-item-list]");
	if (!itemList) {
		return;
	}

	const addressBox = document.querySelector("[data-address-box]");
	const addressFormSlide = document.querySelector("[data-address-form-slide]");
	const addressForm = document.querySelector("[data-address-form]");
	const addressFormMessage = document.querySelector("[data-address-form-message]");
	const addressFormCancel = document.querySelector("[data-address-form-cancel]");
	const productAmountElement = document.querySelector("[data-product-amount]");
	const totalAmountElement = document.querySelector("[data-total-amount]");
	const agreeCheckbox = document.querySelector("[data-agree-checkbox]");
	const payButton = document.querySelector("[data-pay-button]");
	const feedback = document.querySelector("#cart-feedback");
	const pageData = document.body.dataset;

	const state = {
		productAmount: 0,
		addresses: [],
		selectedAddressId: null
	};

	let feedbackTimer = null;

	const formatPrice = (price) => new Intl.NumberFormat("ko-KR").format(Number(price)) + "원";

	const showFeedback = (message, isError = false) => {
		if (!feedback) {
			return;
		}
		window.clearTimeout(feedbackTimer);
		feedback.textContent = message;
		feedback.classList.toggle("is-error", isError);
		feedback.hidden = false;
		feedbackTimer = window.setTimeout(() => {
			feedback.hidden = true;
		}, 3000);
	};

	const csrfHeaders = () => {
		const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
		const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
		const headers = { "Content-Type": "application/json", Accept: "application/json" };
		if (csrfToken && csrfHeader) {
			headers[csrfHeader] = csrfToken;
		}
		return headers;
	};

	const request = async (url, options) => {
		const response = await fetch(url, { headers: csrfHeaders(), ...options });
		if (response.status === 401 || response.redirected) {
			window.location.assign("/login");
			return null;
		}
		if (!response.ok) {
			const result = await response.json().catch(() => ({}));
			throw new Error(result.message || result.detail || "요청을 처리하지 못했습니다.");
		}
		return response.headers.get("content-length") === "0" ? null : response.json().catch(() => null);
	};

	const renderItems = (cart) => {
		itemList.innerHTML = "";
		let total = 0;

		cart.products.filter((item) => item.selected).forEach((item) => {
			total += Number(item.lineTotal);
			const li = document.createElement("li");
			li.className = "cart-line-item";
			const image = document.createElement("img");
			image.className = "cart-line-thumb";
			image.alt = `${item.name} 상품 이미지`;
			image.src = item.imageUrl || "/images/products/product-placeholder.svg";
			const info = document.createElement("div");
			info.className = "cart-line-info";
			const name = document.createElement("h3");
			name.textContent = item.name;
			const meta = document.createElement("p");
			meta.textContent = `${item.category} · 수량 ${item.quantity}`;
			info.append(name, meta);
			const price = document.createElement("strong");
			price.className = "cart-line-price";
			price.textContent = formatPrice(item.lineTotal);
			li.append(image, info, price);
			itemList.appendChild(li);
		});

		cart.quotes.filter((item) => item.selected).forEach((item) => {
			total += Number(item.totalPrice);
			const li = document.createElement("li");
			li.className = "cart-line-item";
			const info = document.createElement("div");
			info.className = "cart-line-info";
			const name = document.createElement("h3");
			name.textContent = item.quoteName;
			const meta = document.createElement("p");
			meta.textContent = item.compatible
				? `${item.itemCount}개 부품 · 호환성 검사 통과`
				: `${item.itemCount}개 부품 · 호환성 문제 ${item.compatibilityIssueCount}건`;
			info.append(name, meta);
			const price = document.createElement("strong");
			price.className = "cart-line-price";
			price.textContent = formatPrice(item.totalPrice);
			li.append(info, price);
			itemList.appendChild(li);
		});

		state.productAmount = total;
		if (productAmountElement) productAmountElement.textContent = formatPrice(total);
		if (totalAmountElement) totalAmountElement.textContent = formatPrice(total);
	};

	const renderAddress = () => {
		if (!addressBox) return;
		addressBox.innerHTML = "";

		if (state.addresses.length === 0) {
			const empty = document.createElement("div");
			empty.className = "address-empty";
			const message = document.createElement("p");
			message.textContent = "등록된 배송지가 없습니다.";
			const hint = document.createElement("p");
			hint.textContent = "주문을 진행하려면 배송지를 먼저 등록해 주세요.";
			const registerButton = document.createElement("button");
			registerButton.type = "button";
			registerButton.className = "line-button";
			registerButton.textContent = "+ 배송지 등록";
			registerButton.addEventListener("click", () => openAddressForm());
			empty.append(message, hint, registerButton);
			addressBox.appendChild(empty);
			state.selectedAddressId = null;
			return;
		}

		if (!state.selectedAddressId) {
			const defaultAddress = state.addresses.find((a) => a.isDefault) || state.addresses[0];
			state.selectedAddressId = defaultAddress.addressId;
		}

		const select = document.createElement("select");
		select.setAttribute("aria-label", "배송지 선택");
		state.addresses.forEach((address) => {
			const option = document.createElement("option");
			option.value = String(address.addressId);
			option.textContent = `${address.addressName} · ${address.recipientName}`;
			option.selected = address.addressId === state.selectedAddressId;
			select.appendChild(option);
		});
		select.addEventListener("change", () => {
			state.selectedAddressId = Number(select.value);
			renderSelectedAddressDetail();
			updatePayButton();
		});

		const detail = document.createElement("div");
		detail.className = "address-detail";
		detail.dataset.addressDetail = "true";

		const actions = document.createElement("div");
		actions.className = "address-box-actions";

		const addNewButton = document.createElement("button");
		addNewButton.type = "button";
		addNewButton.className = "line-button";
		addNewButton.textContent = "+ 새 배송지 등록";
		addNewButton.addEventListener("click", () => openAddressForm());

		const manageLink = document.createElement("a");
		manageLink.className = "line-button";
		manageLink.href = "/mypage/addresses";
		manageLink.textContent = "배송지 관리";

		actions.append(addNewButton, manageLink);
		addressBox.append(select, detail, actions);
		renderSelectedAddressDetail();
	};

	const openAddressForm = () => {
		if (!addressFormSlide) return;
		addressFormSlide.classList.add("is-open");
		addressFormSlide.style.maxHeight = `${addressFormSlide.scrollHeight}px`;
		window.setTimeout(() => addressForm?.querySelector("input[name='recipientName']")?.focus(), 300);
	};

	const closeAddressForm = () => {
		if (!addressFormSlide) return;
		addressFormSlide.classList.remove("is-open");
		addressFormSlide.style.maxHeight = "";
		addressForm?.reset();
		if (addressFormMessage) addressFormMessage.textContent = "";
	};

	const renderSelectedAddressDetail = () => {
		const detail = addressBox?.querySelector("[data-address-detail]");
		if (!detail) return;
		const address = state.addresses.find((a) => a.addressId === state.selectedAddressId);
		detail.innerHTML = "";
		if (!address) return;
		const name = document.createElement("p");
		name.className = "address-name";
		name.textContent = `${address.recipientName} ${address.isDefault ? "(기본 배송지)" : ""}`;
		const phone = document.createElement("p");
		phone.textContent = address.phone;
		const addressLine = document.createElement("p");
		addressLine.textContent = `[${address.zipCode}] ${address.address1} ${address.address2 || ""}`;
		detail.append(name, phone, addressLine);
	};

	const updatePayButton = () => {
		if (!payButton) return;
		if (state.addresses.length === 0) {
			payButton.disabled = true;
			payButton.textContent = "배송지 등록 후 결제하기";
		} else if (pageData.tossConfigured !== "true") {
			payButton.disabled = true;
			payButton.textContent = "결제 연동이 설정되지 않았습니다";
		} else if (!agreeCheckbox?.checked) {
			payButton.disabled = true;
			payButton.textContent = "주문 내용에 동의해 주세요";
		} else if (state.productAmount <= 0) {
			payButton.disabled = true;
			payButton.textContent = "주문할 상품이 없습니다";
		} else {
			payButton.disabled = false;
			payButton.textContent = "결제하기";
		}
	};

	agreeCheckbox?.addEventListener("change", updatePayButton);

	addressFormCancel?.addEventListener("click", () => closeAddressForm());

	addressForm?.querySelector("[data-address-search]")?.addEventListener("click", () => {
		new daum.Postcode({
			oncomplete: data => {
				addressForm.querySelector("input[name='zipCode']").value = data.zonecode;
				addressForm.querySelector("input[name='address1']").value =
					data.roadAddress || data.jibunAddress;
				addressForm.querySelector("input[name='address2']").focus();
			}
		}).open();
	});

	addressForm?.addEventListener("submit", async (event) => {
		event.preventDefault();
		const formData = new FormData(addressForm);
		const submitButton = addressForm.querySelector("button[type='submit']");
		submitButton.disabled = true;
		try {
			const saved = await request("/api/addresses", {
				method: "POST",
				body: JSON.stringify({
					addressName: formData.get("addressName"),
					recipientName: formData.get("recipientName"),
					phone: formData.get("phone"),
					zipCode: formData.get("zipCode"),
					address1: formData.get("address1"),
					address2: formData.get("address2"),
					isDefault: formData.get("isDefault") === "on"
				})
			});
			if (!saved) return;

			if (saved.isDefault) {
				state.addresses = state.addresses.map((address) => ({ ...address, isDefault: false }));
			}
			state.addresses.push(saved);
			state.selectedAddressId = saved.addressId;
			closeAddressForm();
			renderAddress();
			updatePayButton();
			showFeedback("배송지를 등록했습니다.");
		} catch (error) {
			if (addressFormMessage) addressFormMessage.textContent = error.message;
		} finally {
			submitButton.disabled = false;
		}
	});

	const originUrl = () => window.location.origin;

	const payWithToss = async (orderNumber, orderName, amount) => {
		if (typeof TossPayments !== "function") {
			throw new Error("토스페이먼츠 SDK를 불러오지 못했습니다.");
		}
		const tossPayments = TossPayments(pageData.tossClientKey);
		const payment = tossPayments.payment({ customerKey: pageData.customerKey });
		await payment.requestPayment({
			method: "CARD",
			amount: { currency: "KRW", value: Number(amount) },
			orderId: orderNumber,
			orderName,
			successUrl: `${originUrl()}/payments/success`,
			failUrl: `${originUrl()}/payments/fail`,
			card: { flowMode: "DEFAULT" }
		});
	};

	payButton?.addEventListener("click", async () => {
		if (payButton.disabled) return;
		payButton.disabled = true;
		try {
			const order = await request("/api/orders", {
				method: "POST",
				body: JSON.stringify({ addressId: state.selectedAddressId, deliveryRequest: "" })
			});
			if (!order) return;

			const orderName = order.groups.length > 1
				? `${order.groups[0].groupName} 외 ${order.groups.length - 1}건`
				: order.groups[0].groupName;

			await payWithToss(order.orderNumber, orderName, order.finalAmount);
		} catch (error) {
			if (error.code !== "USER_CANCEL") {
				showFeedback(error.message, true);
			}
			payButton.disabled = false;
			updatePayButton();
		}
	});

	const load = async () => {
		try {
			const [cart, addresses] = await Promise.all([
				request("/api/cart"),
				request("/api/addresses")
			]);
			if (cart) renderItems(cart);
			if (addresses) {
				state.addresses = addresses;
				renderAddress();
			}
			updatePayButton();
		} catch (error) {
			showFeedback(error.message, true);
		}
	};

	load();
});
