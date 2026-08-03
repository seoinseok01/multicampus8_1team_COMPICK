package com.boot.compick.order.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.boot.compick.member.entity.Address;
import com.boot.compick.member.entity.Member;
import com.boot.compick.member.service.AddressService;
import com.boot.compick.member.service.MemberService;
import com.boot.compick.member.dto.AddressForm;
import com.boot.compick.cart.service.CartQuoteService;
import com.boot.compick.order.dto.CheckoutItem;
import com.boot.compick.order.dto.CheckoutQuote;
import com.boot.compick.order.dto.CheckoutView;
import com.boot.compick.order.dto.CreateOrderRequest;
import com.boot.compick.order.dto.OrderGroupView;
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
	private static final Object ORDER_NUMBER_LOCK = new Object();
	private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.BASIC_ISO_DATE;
	private final MemberService memberService;
	private final AddressService addressService;
	private final CheckoutService checkoutService;
	private final OrderRepository orderRepository;
	private final OrderGroupRepository groupRepository;
	private final OrderItemRepository itemRepository;
	private final CartQuoteService cartQuoteService;

	@Transactional
	public OrderEntity create(String loginId, CreateOrderRequest request) {
		Member member = memberService.findActiveByLoginId(loginId);
		CheckoutView checkout = checkoutService.getCheckout(member.getId());
		if (checkout.isEmpty()) {
			throw new IllegalArgumentException("주문할 장바구니 상품이 없습니다.");
		}
		AddressValue address = resolveAddress(loginId, request);
		String number = nextOrderNumber();
		OrderEntity order = orderRepository.save(OrderEntity.create(member.getId(), number, checkout.finalAmount(),
			address.name, address.phone, address.zip, address.basic, address.detail, trim(request.getDeliveryRequest())));
		if (!checkout.items().isEmpty()) {
			OrderGroupEntity group = groupRepository.save(OrderGroupEntity.create(order));
			itemRepository.saveAll(checkout.items().stream()
				.map(item -> toOrderItem(order, group, item, item.quantity()))
				.toList());
		}
		for (CheckoutQuote quote : checkout.quotes()) {
			OrderGroupEntity group = groupRepository.save(
				OrderGroupEntity.createQuote(order, quote.quoteId(), quote.name(), quote.quantity()));
			itemRepository.saveAll(quote.items().stream()
				.map(item -> toOrderItem(order, group, item, item.quantity() * quote.quantity()))
				.toList());
		}
		cartQuoteService.removeSelected(member.getId());
		return order;
	}

	public OrderEntity findOwned(String loginId, String orderNumber) {
		Long memberId = memberService.findActiveByLoginId(loginId).getId();
		return orderRepository.findByOrderNumberAndMemberId(orderNumber, memberId)
			.orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
	}
	public List<OrderEntity> findAllOwned(String loginId) {
		Long memberId = memberService.findActiveByLoginId(loginId).getId();
		return orderRepository.findAllByMemberIdOrderByOrderedAtDesc(memberId);
	}
	public List<OrderGroupView> getGroups(Long orderId) {
		var items = itemRepository.findAllByOrderIdOrderById(orderId);
		Map<Long, List<OrderItemEntity>> itemsByGroup = items.stream()
			.collect(Collectors.groupingBy(item -> item.getGroup().getId(), LinkedHashMap::new, Collectors.toList()));
		return groupRepository.findAllByOrderIdOrderById(orderId).stream().map(group -> {
			List<OrderItemEntity> groupItems = itemsByGroup.getOrDefault(group.getId(), List.of());
			long amount = groupItems.stream().mapToLong(OrderItemEntity::getLineAmount).sum();
			return new OrderGroupView(group.getId(), group.getGroupName(), group.getGroupType(),
				group.getGroupQuantity(), groupItems, amount);
		}).toList();
	}
	public List<OrderItemEntity> getItems(Long orderId) {
		return itemRepository.findAllByOrderIdOrderById(orderId);
	}

	@Transactional
	public void complete(OrderEntity order) {
		order.markPaid();
	}

	@Transactional
	public void cancelPendingAndRestore(String loginId, OrderEntity order) {
		if (order.getStatus() != com.boot.compick.order.entity.OrderStatus.PAYMENT_PENDING)
			throw new IllegalArgumentException("결제 대기 주문만 장바구니로 되돌릴 수 있습니다.");
		groupRepository.findAllByOrderIdOrderById(order.getId()).stream()
			.filter(group -> "QUOTE".equals(group.getGroupType()) && group.getSourceQuoteId() != null)
			.forEach(group -> cartQuoteService.add(loginId, group.getSourceQuoteId()));
		itemRepository.deleteAllByOrderId(order.getId());
		groupRepository.deleteAllByOrderId(order.getId());
		orderRepository.delete(order);
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
	private String nextOrderNumber() {
		synchronized (ORDER_NUMBER_LOCK) {
			String prefix = "cp" + LocalDate.now().format(ORDER_DATE);
			int sequence = orderRepository.findTopByOrderNumberStartingWithOrderByOrderNumberDesc(prefix)
				.map(order -> Integer.parseInt(order.getOrderNumber().substring(prefix.length())))
				.orElse(0) + 1;
			if (sequence > 999) throw new IllegalStateException("오늘 생성할 수 있는 주문번호를 초과했습니다.");
			return prefix + String.format("%03d", sequence);
		}
	}
	private OrderItemEntity toOrderItem(OrderEntity order, OrderGroupEntity group, CheckoutItem item, int quantity) {
		return OrderItemEntity.create(order, group, item.productId(), item.name(), item.price(), quantity);
	}
	private record AddressValue(String name, String phone, String zip, String basic, String detail) {}
}
