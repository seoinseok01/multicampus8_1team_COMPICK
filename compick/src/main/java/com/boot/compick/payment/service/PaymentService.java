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
import com.boot.compick.payment.client.KakaoPayClient;
import com.boot.compick.payment.dto.KakaoApproveResponse;
import com.boot.compick.payment.dto.KakaoReadyResponse;
import com.boot.compick.payment.entity.PaymentEntity;
import com.boot.compick.payment.entity.PaymentMethod;
import com.boot.compick.payment.entity.PaymentStatus;
import com.boot.compick.payment.repository.PaymentRepository;

import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PaymentService {

	private final OrderRepository orderRepository;
	private final PaymentRepository paymentRepository;
	private final KakaoPayClient kakaoPayClient;
	private final TossPaymentService tossPaymentService;
	private final CartService cartService;

	public PaymentService(
		OrderRepository orderRepository,
		PaymentRepository paymentRepository,
		KakaoPayClient kakaoPayClient,
		TossPaymentService tossPaymentService,
		CartService cartService
	) {
		this.orderRepository = orderRepository;
		this.paymentRepository = paymentRepository;
		this.kakaoPayClient = kakaoPayClient;
		this.tossPaymentService = tossPaymentService;
		this.cartService = cartService;
	}

	public boolean isKakaoConfigured() {
		return kakaoPayClient.isConfigured();
	}

	public boolean isTossConfigured() {
		return tossPaymentService.isConfigured();
	}

	public String tossClientKey() {
		return tossPaymentService.clientKey();
	}

	@Transactional
	public String readyKakao(String loginId, String orderNumber, Long memberId, String originUrl) {
		OrderEntity order = requireOwnedPendingOrder(orderNumber, memberId);

		KakaoReadyResponse ready = kakaoPayClient.ready(
			orderNumber,
			"member-" + memberId,
			representativeItemName(order),
			1,
			order.getFinalAmount(),
			originUrl + "/payments/kakao/approve?orderNumber=" + orderNumber,
			originUrl + "/payments/kakao/cancel?orderNumber=" + orderNumber,
			originUrl + "/payments/kakao/fail?orderNumber=" + orderNumber
		);

		paymentRepository.findByOrderId(order.getOrderId()).ifPresentOrElse(
			existing -> existing.changeExternalTransactionId(ready.tid()),
			() -> paymentRepository.save(PaymentEntity.createReady(
				order.getOrderId(), PaymentMethod.KAKAO_PAY, order.getFinalAmount(), ready.tid()
			))
		);

		return ready.nextRedirectPcUrl();
	}

	@Transactional
	public void approveKakao(String loginId, String orderNumber, String pgToken, Long memberId) {
		OrderEntity order = requireOwnedPendingOrder(orderNumber, memberId);
		PaymentEntity payment = paymentRepository.findByOrderId(order.getOrderId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 준비 정보를 찾을 수 없습니다."));

		KakaoApproveResponse approved = kakaoPayClient.approve(
			payment.getExternalTransactionId(),
			orderNumber,
			"member-" + memberId,
			pgToken
		);

		if (approved.amount() == null || approved.amount().total() != order.getFinalAmount()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "결제 금액이 주문 금액과 일치하지 않습니다.");
		}

		payment.approve(approved.aid());
		order.markPaid();
		clearOrderedCartItems(loginId, order);
	}

	@Transactional
	public void markKakaoNotApproved(String orderNumber, Long memberId, boolean cancelled) {
		OrderEntity order = requireOwnedPendingOrder(orderNumber, memberId);
		paymentRepository.findByOrderId(order.getOrderId()).ifPresent(payment -> {
			if (cancelled) {
				payment.cancel();
			} else {
				payment.fail();
			}
		});
	}

	@Transactional
	public void confirmToss(String loginId, String orderNumber, String paymentKey, long amount, Long memberId) {
		OrderEntity order = requireOwnedPendingOrder(orderNumber, memberId);
		if (order.getFinalAmount() != amount) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "결제 금액이 주문 금액과 일치하지 않습니다.");
		}

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
		if (payment.getPaymentMethod() == PaymentMethod.KAKAO_PAY) {
			kakaoPayClient.cancel(payment.getExternalTransactionId(), payment.getPaymentAmount());
		} else {
			tossPaymentService.cancel(payment.getExternalTransactionId(), "고객 요청에 의한 주문 취소");
		}
		payment.cancel();
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

	private String representativeItemName(OrderEntity order) {
		if (order.getGroups().isEmpty()) {
			return order.getOrderNumber();
		}
		String first = order.getGroups().get(0).getGroupName();
		int others = order.getGroups().size() - 1;
		return others > 0 ? first + " 외 " + others + "건" : first;
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
