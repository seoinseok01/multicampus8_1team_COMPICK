package com.boot.compick.order.service;

import java.util.UUID;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.boot.compick.member.entity.Address;
import com.boot.compick.member.entity.Member;
import com.boot.compick.member.service.AddressService;
import com.boot.compick.member.service.MemberService;
import com.boot.compick.member.dto.AddressForm;
import com.boot.compick.order.dto.CheckoutItem;
import com.boot.compick.order.dto.CheckoutView;
import com.boot.compick.order.dto.CreateOrderRequest;
import com.boot.compick.order.entity.OrderEntity;
import com.boot.compick.order.entity.OrderGroupEntity;
import com.boot.compick.order.entity.OrderItemEntity;
import com.boot.compick.order.repository.OrderGroupRepository;
import com.boot.compick.order.repository.OrderItemRepository;
import com.boot.compick.order.repository.OrderRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
	private final MemberService memberService;
	private final AddressService addressService;
	private final CheckoutService checkoutService;
	private final OrderRepository orderRepository;
	private final OrderGroupRepository groupRepository;
	private final OrderItemRepository itemRepository;

	@Transactional
	public OrderEntity create(String loginId, CreateOrderRequest request) {
		Member member = memberService.findActiveByLoginId(loginId);
		CheckoutView checkout = checkoutService.getCheckout(member.getId());
		if (checkout.items().isEmpty()) {
			throw new IllegalArgumentException("주문할 장바구니 상품이 없습니다.");
		}
		AddressValue address = resolveAddress(loginId, request);
		String number = "COMPICK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
		OrderEntity order = orderRepository.save(OrderEntity.create(member.getId(), number, checkout.finalAmount(),
			address.name, address.phone, address.zip, address.basic, address.detail, trim(request.getDeliveryRequest())));
		OrderGroupEntity group = groupRepository.save(OrderGroupEntity.create(order));
		itemRepository.saveAll(checkout.items().stream()
			.map(item -> toOrderItem(order, group, item))
			.toList());
		return order;
	}

	public OrderEntity findOwned(String loginId, String orderNumber) {
		Long memberId = memberService.findActiveByLoginId(loginId).getId();
		return orderRepository.findByOrderNumberAndMemberId(orderNumber, memberId)
			.orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
	}
	public List<OrderItemEntity> getItems(Long orderId) {
		return itemRepository.findAllByOrderIdOrderById(orderId);
	}

	@Transactional
	public void complete(OrderEntity order) {
		order.markPaid();
	}

	private AddressValue resolveAddress(String loginId, CreateOrderRequest request) {
		if (request.getAddressId() != null) {
			Address a = addressService.findOwned(loginId, request.getAddressId());
			return new AddressValue(a.getRecipientName(), a.getRecipientPhone(), a.getZipCode(), a.getBasicAddress(), a.getDetailAddress());
		}
		if (blank(request.getRecipientName()) || blank(request.getRecipientPhone()) || blank(request.getZipCode()) || blank(request.getBasicAddress()))
			throw new IllegalArgumentException("받는 사람, 연락처, 우편번호와 주소를 입력해 주세요.");
		if (request.isSaveAddress()) {
			AddressForm form = new AddressForm();
			form.setAddressName(blank(request.getAddressName()) ? "배송지" : request.getAddressName().trim());
			form.setRecipientName(request.getRecipientName().trim()); form.setRecipientPhone(request.getRecipientPhone().trim());
			form.setZipCode(request.getZipCode().trim()); form.setBasicAddress(request.getBasicAddress().trim());
			form.setDetailAddress(trim(request.getDetailAddress())); form.setDefaultAddress(request.isDefaultAddress());
			addressService.save(loginId, null, form);
		}
		return new AddressValue(request.getRecipientName().trim(), request.getRecipientPhone().trim(),
			request.getZipCode().trim(), request.getBasicAddress().trim(), trim(request.getDetailAddress()));
	}
	private boolean blank(String value) { return value == null || value.isBlank(); }
	private String trim(String value) { return value == null ? null : value.trim(); }
	private OrderItemEntity toOrderItem(OrderEntity order, OrderGroupEntity group, CheckoutItem item) {
		return OrderItemEntity.create(order, group, item.productId(), item.name(), item.price(), item.quantity());
	}
	private record AddressValue(String name, String phone, String zip, String basic, String detail) {}
}
