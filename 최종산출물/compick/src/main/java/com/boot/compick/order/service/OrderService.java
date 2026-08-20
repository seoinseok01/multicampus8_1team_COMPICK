package com.boot.compick.order.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.boot.compick.cart.entity.CartProductItemEntity;
import com.boot.compick.cart.entity.CartQuoteItemEntity;
import com.boot.compick.cart.repository.CartMemberLookupRepository;
import com.boot.compick.cart.service.CartService;
import com.boot.compick.member.entity.Address;
import com.boot.compick.member.repository.AddressRepository;
import com.boot.compick.order.dto.CreateOrderRequest;
import com.boot.compick.order.dto.OrderDetailResponse;
import com.boot.compick.order.dto.OrderGroupResponse;
import com.boot.compick.order.dto.OrderSummaryResponse;
import com.boot.compick.order.entity.OrderEntity;
import com.boot.compick.order.entity.OrderGroupEntity;
import com.boot.compick.order.entity.OrderStatus;
import com.boot.compick.order.repository.OrderRepository;
import com.boot.compick.payment.entity.PaymentEntity;
import com.boot.compick.payment.repository.PaymentRepository;
import com.boot.compick.payment.service.PaymentService;
import com.boot.compick.product.entity.ProductEntity;
import com.boot.compick.product.repository.ProductRepository;
import com.boot.compick.quote.entity.QuoteEntity;
import com.boot.compick.quote.entity.QuoteItemEntity;
import com.boot.compick.quote.repository.QuoteRepository;

@Service
@Transactional(readOnly = true)
public class OrderService {

	private static final DateTimeFormatter ORDER_NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final OrderRepository orderRepository;
	private final PaymentRepository paymentRepository;
	private final CartService cartService;
	private final CartMemberLookupRepository memberLookupRepository;
	private final AddressRepository addressRepository;
	private final ProductRepository productRepository;
	private final QuoteRepository quoteRepository;
	private final PaymentService paymentService;

	public OrderService(
		OrderRepository orderRepository,
		PaymentRepository paymentRepository,
		CartService cartService,
		CartMemberLookupRepository memberLookupRepository,
		AddressRepository addressRepository,
		ProductRepository productRepository,
		QuoteRepository quoteRepository,
		PaymentService paymentService
	) {
		this.orderRepository = orderRepository;
		this.paymentRepository = paymentRepository;
		this.cartService = cartService;
		this.memberLookupRepository = memberLookupRepository;
		this.addressRepository = addressRepository;
		this.productRepository = productRepository;
		this.quoteRepository = quoteRepository;
		this.paymentService = paymentService;
	}

	@Transactional
	public OrderDetailResponse createPendingOrder(String loginId, CreateOrderRequest request) {
		Long memberId = activeMemberId(loginId);

		List<CartProductItemEntity> selectedProducts = cartService.findSelectedProductItems(loginId);
		List<CartQuoteItemEntity> selectedQuotes = cartService.findSelectedQuoteItems(loginId);
		if (selectedProducts.isEmpty() && selectedQuotes.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "주문할 상품을 장바구니에서 선택해 주세요.");
		}

		Address address = addressRepository.findByIdAndMemberId(request.addressId(), memberId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "배송지를 찾을 수 없습니다."));

		Map<Long, ProductEntity> selectedProductsById = productRepository.findAllById(
			selectedProducts.stream().map(CartProductItemEntity::getProductId).toList()
		).stream().collect(Collectors.toMap(ProductEntity::getProductId, Function.identity()));

		long productAmount = 0;
		for (CartProductItemEntity item : selectedProducts) {
			ProductEntity product = selectedProductsById.get(item.getProductId());
			if (product != null) {
				productAmount += product.getPrice() * item.getQuantity();
			}
		}

		record QuoteBundle(QuoteEntity quote, CartQuoteItemEntity cartItem, Map<Long, ProductEntity> products, long total) {
		}
		Map<Long, QuoteEntity> quotesById = quoteRepository.findByQuoteIdIn(
			selectedQuotes.stream().map(CartQuoteItemEntity::getQuoteId).distinct().toList()
		).stream().collect(Collectors.toMap(QuoteEntity::getQuoteId, Function.identity()));
		Map<Long, ProductEntity> quoteProductsById = productRepository.findAllById(
			quotesById.values().stream()
				.flatMap(quote -> quote.getItems().stream())
				.map(QuoteItemEntity::getProductId)
				.distinct()
				.toList()
		).stream().collect(Collectors.toMap(ProductEntity::getProductId, Function.identity()));

		List<QuoteBundle> quoteBundles = selectedQuotes.stream()
			.map(cartItem -> {
				QuoteEntity quote = quotesById.get(cartItem.getQuoteId());
				if (quote == null) {
					return null;
				}
					long total = quote.getItems().stream()
						.mapToLong(qi -> {
							ProductEntity p = quoteProductsById.get(qi.getProductId());
							return p == null ? 0L : p.getPrice() * qi.getQuantity();
						}).sum() * cartItem.getQuantity();
				return new QuoteBundle(quote, cartItem, quoteProductsById, total);
			})
			.filter(Objects::nonNull)
			.toList();

		productAmount += quoteBundles.stream().mapToLong(QuoteBundle::total).sum();

		String orderNumber = generateOrderNumber();
		OrderEntity order = OrderEntity.create(
			memberId,
			orderNumber,
			productAmount,
			0,
			address.getRecipientName(),
			address.getRecipientPhone(),
			address.getZipCode(),
			address.getBasicAddress(),
			address.getDetailAddress(),
			blankToNull(request.deliveryRequest())
		);

		for (CartProductItemEntity item : selectedProducts) {
			ProductEntity product = selectedProductsById.get(item.getProductId());
			if (product != null) {
				order.addProductGroup(product.getProductId(), product.getProductName(), product.getPrice(), item.getQuantity());
			}
		}

		for (QuoteBundle bundle : quoteBundles) {
			OrderGroupEntity group = order.addQuoteGroup(
				bundle.quote().getQuoteId(),
				bundle.quote().getQuoteName(),
				bundle.cartItem().getQuantity(),
				bundle.quote().getAssemblyType().name()
			);
			for (QuoteItemEntity quoteItem : bundle.quote().getItems()) {
				ProductEntity product = bundle.products().get(quoteItem.getProductId());
				if (product != null) {
					group.addItem(product.getProductId(), product.getProductName(), product.getPrice(), quoteItem.getQuantity());
				}
			}
		}

		OrderEntity saved = orderRepository.save(order);
		return toDetail(saved, null);
	}

	public OrderDetailResponse findOrderDetail(String loginId, String orderNumber) {
		OrderEntity order = findOwnedOrder(loginId, orderNumber);
		PaymentEntity payment = paymentRepository.findByOrderId(order.getOrderId()).orElse(null);
		return toDetail(order, payment);
	}

	public List<OrderSummaryResponse> findOrders(String loginId, OrderStatus statusFilter) {
		Long memberId = activeMemberId(loginId);
		return orderRepository.findByMemberIdOrderByOrderedAtDesc(memberId).stream()
			.filter(order -> matchesHistoryFilter(order, statusFilter))
			.map(this::toSummary)
			.toList();
	}

	private boolean matchesHistoryFilter(OrderEntity order, OrderStatus statusFilter) {
		if (statusFilter == null) return true;
		if (statusFilter == OrderStatus.CANCELLED) {
			return order.getOrderStatus() == OrderStatus.CANCELLED || order.isReturnRequested();
		}
		return !order.isReturnRequested() && order.getOrderStatus() == statusFilter;
	}

	@Transactional
	public void requestCancel(String loginId, String orderNumber) {
		OrderEntity order = findOwnedOrder(loginId, orderNumber);
		if (!order.isCancellable()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 배송이 진행되었거나 취소할 수 없는 주문입니다.");
		}
		if (order.getOrderStatus() == OrderStatus.PAID || order.getOrderStatus() == OrderStatus.PREPARING) {
			paymentService.cancelForOrder(order);
		}
		order.cancel();
	}

	/**
	 * 반품 요청. ORDERS.order_status에는 "반품 요청됨" 같은 중간 상태가 없어서(스키마 변경 없이는
	 * 추가 불가) 실제 상태 전이 없이 소유자 확인만 하고 접수 여부만 응답한다. 반품 처리 상태를
	 * 추적하려면 스키마에 컬럼을 추가하는 논의가 먼저 필요하다.
	 */
	@Transactional
	public void requestReturn(String loginId, String orderNumber) {
		OrderEntity order = findOwnedOrder(loginId, orderNumber);
		if (order.getOrderStatus() != OrderStatus.SHIPPING && order.getOrderStatus() != OrderStatus.DELIVERED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "배송 중이거나 배송 완료된 주문만 반품할 수 있습니다.");
		}
		if (order.isReturnRequested()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 반품이 요청된 주문입니다.");
		}
		paymentService.refundHalfForOrder(order);
		order.requestReturn();
	}

	/** PaymentService의 결제 승인 처리에서 소유자 확인 없이 orderNumber로 바로 찾을 때 쓴다. */
	public OrderEntity findByOrderNumber(String orderNumber) {
		return orderRepository.findByOrderNumber(orderNumber)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
	}

	private OrderEntity findOwnedOrder(String loginId, String orderNumber) {
		Long memberId = activeMemberId(loginId);
		return orderRepository.findByOrderNumberAndMemberId(orderNumber, memberId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
	}

	private OrderSummaryResponse toSummary(OrderEntity order) {
		List<OrderGroupEntity> groups = order.getGroups();
		String title = groups.isEmpty() ? "" : groups.get(0).getGroupName();
		int otherCount = groups.size() - 1;
		if (otherCount > 0) {
			title = title + " 외 " + otherCount + "건";
		}
		return new OrderSummaryResponse(
			order.getOrderNumber(),
			order.getOrderedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
			order.isReturnRequested() ? "RETURN_COMPLETED" : order.getOrderStatus().name(),
			order.isReturnRequested() ? "반품완료" : statusLabel(order.getOrderStatus()),
			title,
			order.getFinalAmount()
		);
	}

	private OrderDetailResponse toDetail(OrderEntity order, PaymentEntity payment) {
		List<OrderGroupResponse> groups = order.getGroups().stream()
			.map(group -> new OrderGroupResponse(
				group.getGroupType().name(),
				group.getGroupName(),
				group.getGroupType() == com.boot.compick.order.entity.OrderGroupType.QUOTE
					? "%d개 부품 · 수량 %d".formatted(group.getItems().size(), group.getGroupQuantity())
					: "수량 %d".formatted(group.getGroupQuantity()),
				group.getItems().stream().mapToLong(item -> item.getLineAmount()).sum()
			))
			.toList();

		String fullAddress = order.getDetailAddress() == null || order.getDetailAddress().isBlank()
			? order.getBasicAddress()
			: order.getBasicAddress() + " " + order.getDetailAddress();

		return new OrderDetailResponse(
			order.getOrderNumber(),
			order.getOrderedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")),
			order.isReturnRequested() ? "RETURN_COMPLETED" : order.getOrderStatus().name(),
			order.isReturnRequested() ? "반품완료" : statusLabel(order.getOrderStatus()),
			groups,
			order.getProductAmount(),
			order.getShippingFee(),
			order.getFinalAmount(),
			order.getRecipientName(),
			maskPhone(order.getRecipientPhone()),
			fullAddress,
			order.getDeliveryRequest(),
			payment == null ? null : paymentMethodLabel(payment.getPaymentMethod().name()),
			payment == null ? null : payment.getPaymentAmount(),
			order.isCancellable(),
			order.isReturnRequested()
		);
	}

	private String statusLabel(OrderStatus status) {
		return switch (status) {
			case PAYMENT_PENDING -> "결제 대기";
			case PAID -> "결제 완료";
			case PREPARING -> "배송 준비중";
			case SHIPPING -> "배송 중";
			case DELIVERED -> "배송 완료";
			case CANCELLED -> "주문 취소";
		};
	}

	private String paymentMethodLabel(String method) {
		return switch (method) {
			case "TOSS" -> "토스페이먼츠";
			default -> method;
		};
	}

	private String maskPhone(String phone) {
		if (phone == null || phone.length() < 8) {
			return phone;
		}
		String digitsOnly = phone.replace("-", "");
		if (digitsOnly.length() < 8) {
			return phone;
		}
		String middle = "*".repeat(digitsOnly.length() - 7);
		return digitsOnly.substring(0, 3) + "-" + middle + "-" + digitsOnly.substring(digitsOnly.length() - 4);
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String generateOrderNumber() {
		LocalDate today = LocalDate.now();
		LocalDateTime startOfDay = today.atStartOfDay();
		LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
		long countToday = orderRepository.countByOrderedAtBetween(startOfDay, endOfDay);
		return "CP" + today.format(ORDER_NUMBER_DATE) + "%03d".formatted(countToday + 1);
	}

	private Long activeMemberId(String loginId) {
		return memberLookupRepository.findActiveMemberIdByLoginId(loginId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "로그인한 회원 정보를 찾을 수 없습니다."));
	}
}
