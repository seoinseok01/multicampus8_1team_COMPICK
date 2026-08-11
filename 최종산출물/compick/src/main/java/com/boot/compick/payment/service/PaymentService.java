package com.boot.compick.payment.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.boot.compick.cart.service.CartService;
import com.boot.compick.order.entity.OrderEntity;
import com.boot.compick.order.entity.OrderGroupEntity;
import com.boot.compick.order.entity.OrderGroupType;
import com.boot.compick.order.entity.OrderStatus;
import com.boot.compick.order.repository.OrderRepository;
import com.boot.compick.payment.entity.PaymentEntity;
import com.boot.compick.payment.entity.PaymentMethod;
import com.boot.compick.payment.entity.PaymentStatus;
import com.boot.compick.payment.repository.PaymentRepository;
import com.boot.compick.product.entity.ProductEntity;
import com.boot.compick.product.repository.ProductRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PaymentService {

	private final OrderRepository orderRepository;
	private final PaymentRepository paymentRepository;
	private final TossPaymentService tossPaymentService;
	private final CartService cartService;
	private final ProductRepository productRepository;

	public PaymentService(
		OrderRepository orderRepository,
		PaymentRepository paymentRepository,
		TossPaymentService tossPaymentService,
		CartService cartService,
		ProductRepository productRepository
	) {
		this.orderRepository = orderRepository;
		this.paymentRepository = paymentRepository;
		this.tossPaymentService = tossPaymentService;
		this.cartService = cartService;
		this.productRepository = productRepository;
	}

	public boolean isTossConfigured() {
		return tossPaymentService.isConfigured();
	}

	public String tossClientKey() {
		return tossPaymentService.clientKey();
	}

	@Transactional
	public void confirmToss(String loginId, String orderNumber, String paymentKey, long amount, Long memberId) {
		OrderEntity order = requireOwnedPendingOrder(orderNumber, memberId);
		if (order.getFinalAmount() != amount) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "결제 금액이 주문 금액과 일치하지 않습니다.");
		}

		decreaseOrderStock(order);
		Map<String, Object> confirmed = tossPaymentService.confirm(paymentKey, orderNumber, amount);

		PaymentEntity payment = paymentRepository.findByOrderId(order.getOrderId())
			.orElseGet(() -> paymentRepository.save(
				PaymentEntity.createReady(order.getOrderId(), PaymentMethod.TOSS, amount, paymentKey)
			));
		payment.changeExternalTransactionId(paymentKey);
		payment.approve(String.valueOf(confirmed.getOrDefault("method", "TOSS")));

		order.markPaid();
		clearOrderedCartItems(loginId, order);
	}

	@Transactional
	public void cancelForOrder(OrderEntity order) {
		PaymentEntity payment = paymentRepository.findByOrderId(order.getOrderId()).orElse(null);
		if (payment == null || payment.getPaymentStatus() != PaymentStatus.APPROVED) {
			return;
		}
		tossPaymentService.cancel(payment.getExternalTransactionId(), "고객 요청에 의한 주문 취소");
		payment.cancel();
		restoreOrderStock(order);
	}

	@Transactional
	public void refundHalfForOrder(OrderEntity order) {
		PaymentEntity payment = paymentRepository.findByOrderId(order.getOrderId()).orElse(null);
		if (payment == null || payment.getPaymentStatus() != PaymentStatus.APPROVED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "환불할 결제 정보를 찾을 수 없습니다.");
		}
		long refundAmount = order.getFinalAmount() / 2;
		long balanceAmount = tossPaymentService.balanceAmount(payment.getExternalTransactionId());
		long alreadyRefunded = Math.max(0, payment.getPaymentAmount() - balanceAmount);
		long remainingRefund = Math.max(0, refundAmount - alreadyRefunded);
		if (remainingRefund > 0) {
			tossPaymentService.cancel(payment.getExternalTransactionId(), "배송 상품 반품 요청", remainingRefund);
		}
		payment.cancel();
		restoreOrderStock(order);
	}

	private void decreaseOrderStock(OrderEntity order) {
		if (order.isStockDeducted()) {
			return;
		}
		Map<Long, Integer> quantities = orderProductQuantities(order);
		Map<Long, ProductEntity> productMap = productsById(quantities);
		if (productMap.size() != quantities.size()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "주문 상품을 찾을 수 없습니다.");
		}
		quantities.forEach((productId, quantity) -> productMap.get(productId).decreaseStock(quantity));
		order.markStockDeducted();
	}

	private void restoreOrderStock(OrderEntity order) {
		if (!order.isStockDeducted() || order.isStockRestored()) {
			return;
		}
		Map<Long, Integer> quantities = orderProductQuantities(order);
		Map<Long, ProductEntity> productMap = productsById(quantities);
		quantities.forEach((productId, quantity) -> {
			ProductEntity product = productMap.get(productId);
			if (product != null) {
				product.increaseStock(quantity);
			}
		});
		order.markStockRestored();
	}

	private Map<Long, ProductEntity> productsById(Map<Long, Integer> quantities) {
		return productRepository.findAllById(quantities.keySet()).stream()
			.collect(Collectors.toMap(ProductEntity::getProductId, product -> product));
	}

	private Map<Long, Integer> orderProductQuantities(OrderEntity order) {
		Map<Long, Integer> quantities = new LinkedHashMap<>();
		for (OrderGroupEntity group : order.getGroups()) {
			int groupMultiplier = group.getGroupType() == OrderGroupType.QUOTE ? group.getGroupQuantity() : 1;
			group.getItems().forEach(item -> quantities.merge(item.getProductId(), item.getQuantity() * groupMultiplier, Integer::sum));
		}
		return quantities;
	}

	private void clearOrderedCartItems(String loginId, OrderEntity order) {
		for (OrderGroupEntity group : order.getGroups()) {
			if (group.getGroupType() == OrderGroupType.PRODUCT) {
				group.getItems().forEach(item -> safeDeleteProduct(loginId, item.getProductId()));
			} else if (group.getGroupType() == OrderGroupType.QUOTE && group.getSourceQuoteId() != null) {
				safeDeleteQuote(loginId, group.getSourceQuoteId());
			}
		}
	}

	private void safeDeleteProduct(String loginId, Long productId) {
		try {
			cartService.deleteProductItem(loginId, productId);
		} catch (ResponseStatusException ignored) {
			// 이미 장바구니에서 지워졌거나(다른 주문/직접 삭제) 없는 경우는 무시한다.
		}
	}

	private void safeDeleteQuote(String loginId, Long quoteId) {
		try {
			cartService.deleteQuoteItem(loginId, quoteId);
		} catch (ResponseStatusException ignored) {
			// 위와 동일한 이유로 무시한다.
		}
	}

	private OrderEntity requireOwnedPendingOrder(String orderNumber, Long memberId) {
		OrderEntity order = orderRepository.findByOrderNumber(orderNumber)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
		if (!order.getMemberId().equals(memberId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다.");
		}
		if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리된 주문입니다.");
		}
		return order;
	}
}
