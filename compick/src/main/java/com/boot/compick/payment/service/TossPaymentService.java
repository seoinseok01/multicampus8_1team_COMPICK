package com.boot.compick.payment.service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

@Service
public class TossPaymentService {
	private final String secretKey; private final String confirmUrl; private final RestClient client;
	public TossPaymentService(@Value("${toss.payments.secret-key:}") String secretKey,
		@Value("${toss.payments.confirm-url:https://api.tosspayments.com/v1/payments/confirm}") String confirmUrl, RestClient.Builder builder) {
		this.secretKey=secretKey; this.confirmUrl=confirmUrl; this.client=builder.build();
	}
	public Map<String,Object> confirm(String paymentKey, String orderId, long amount) {
		if (secretKey.isBlank()) throw new TossPaymentException("토스 시크릿 키가 설정되지 않았습니다.");
		try {
			@SuppressWarnings("unchecked") Map<String,Object> response = client.post().uri(confirmUrl)
				.headers(h -> { h.setBasicAuth(secretKey, ""); h.set("Idempotency-Key",
					UUID.nameUUIDFromBytes(orderId.getBytes(StandardCharsets.UTF_8)).toString()); })
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
				.retrieve().body(Map.class);
			if (response == null) throw new TossPaymentException("토스 결제 승인 응답이 비어 있습니다.");
			return response;
		} catch (RestClientResponseException e) {
			throw new TossPaymentException("토스 결제 승인에 실패했습니다. (" + e.getStatusCode() + ")");
		}
	}
}
