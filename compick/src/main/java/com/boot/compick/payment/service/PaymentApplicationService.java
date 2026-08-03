package com.boot.compick.payment.service;

import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.boot.compick.order.entity.OrderEntity;
import com.boot.compick.order.entity.OrderStatus;
import com.boot.compick.order.service.OrderService;
import com.boot.compick.payment.entity.PaymentEntity;
import com.boot.compick.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentApplicationService {
	public record CancellationResult(long refundedAmount, boolean pendingOrderDeleted) {}
	private final OrderService orderService;
	private final TossPaymentService toss;
	private final PaymentRepository paymentRepository;

	@Transactional
	public Map<String,Object> confirm(String loginId, String paymentKey, String orderNumber, long amount) {
		OrderEntity order = orderService.findOwned(loginId, orderNumber);
		if (order.getFinalAmount() != amount) throw new TossPaymentException("주문 금액이 일치하지 않습니다.");
		if (order.getStatus() != OrderStatus.PAYMENT_PENDING || paymentRepository.existsByOrderId(order.getId()))
			throw new TossPaymentException("이미 처리되었거나 결제할 수 없는 주문입니다.");
		Map<String,Object> result = toss.confirm(paymentKey, orderNumber, amount);
		String method = Objects.toString(result.get("method"), "UNKNOWN");
		String approval = null;
		if (result.get("card") instanceof Map<?,?> card) approval = Objects.toString(card.get("approveNo"), null);
		paymentRepository.save(PaymentEntity.approved(order, method, paymentKey, approval));
		orderService.complete(order);
		return result;
	}

	@Transactional
	public CancellationResult cancel(String loginId, String orderNumber, String reason) {
		OrderEntity order = orderService.findOwned(loginId, orderNumber);
		if (order.getStatus() == OrderStatus.CANCELLED)
			throw new IllegalArgumentException("이미 취소된 주문입니다.");
		if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {
			orderService.cancelPendingAndRestore(loginId, order);
			return new CancellationResult(0, true);
		}
		if (reason == null || reason.isBlank()) throw new IllegalArgumentException("취소 또는 반품 사유를 입력해 주세요.");
		PaymentEntity payment = paymentRepository.findByOrderId(order.getId())
			.orElseThrow(() -> new IllegalArgumentException("승인된 결제 정보를 찾을 수 없습니다."));
		if (!"APPROVED".equals(payment.getStatus()))
			throw new IllegalArgumentException("취소할 수 없는 결제 상태입니다.");
		long refundAmount = order.getStatus() == OrderStatus.PAID
			? order.getFinalAmount() : order.getFinalAmount() / 2;
		Long tossCancelAmount = refundAmount == order.getFinalAmount() ? null : refundAmount;
		toss.cancel(payment.getTransactionId(), orderNumber, reason.trim(), tossCancelAmount);
		payment.cancel(refundAmount, reason.trim());
		order.cancel();
		return new CancellationResult(refundAmount, false);
	}
}
