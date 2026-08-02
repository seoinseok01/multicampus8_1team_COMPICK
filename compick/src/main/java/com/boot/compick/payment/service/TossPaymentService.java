package com.boot.compick.payment.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** 팀원(order-payment 브랜치)의 토스페이먼츠 연동을 그대로 가져왔다. */
@Service
public class TossPaymentService {

	private final String clientKey;
	private final String secretKey;
	private final String confirmUrl;
	private final RestClient restClient;

	public TossPaymentService(
		@Value("${toss.payments.client-key:}") String clientKey,
		@Value("${toss.payments.secret-key:}") String secretKey,
		@Value("${toss.payments.confirm-url:https://api.tosspayments.com/v1/payments/confirm}") String confirmUrl,
		RestClient.Builder restClientBuilder
	) {
		this.clientKey = clientKey;
		this.secretKey = secretKey;
		this.confirmUrl = confirmUrl;
		this.restClient = restClientBuilder.build();
	}

	public boolean isConfigured() {
		return clientKey != null && !clientKey.isBlank();
	}

	public String clientKey() {
		return clientKey;
	}

	public Map<String, Object> confirm(
		String paymentKey,
		String orderId,
		long amount
	) {
		if (secretKey == null || secretKey.isBlank()) {
			throw new TossPaymentException(
				"토스페이먼츠 시크릿 키가 설정되지 않았습니다."
			);
		}

		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> response = restClient.post()
				.uri(confirmUrl)
				.headers(headers -> headers.setBasicAuth(secretKey, ""))
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of(
					"paymentKey", paymentKey,
					"orderId", orderId,
					"amount", amount
				))
				.retrieve()
				.body(Map.class);

			if (response == null) {
				throw new TossPaymentException("결제 승인 응답이 비어 있습니다.");
			}
			return response;
		} catch (RestClientResponseException exception) {
			throw new TossPaymentException(
				"결제 승인에 실패했습니다. (" + exception.getStatusCode() + ")"
			);
		}
	}

	public void cancel(String paymentKey, String cancelReason) {
		if (secretKey == null || secretKey.isBlank()) {
			throw new TossPaymentException("토스페이먼츠 시크릿 키가 설정되지 않았습니다.");
		}
		try {
			restClient.post()
				.uri("https://api.tosspayments.com/v1/payments/{paymentKey}/cancel", paymentKey)
				.headers(headers -> headers.setBasicAuth(secretKey, ""))
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("cancelReason", cancelReason))
				.retrieve()
				.toBodilessEntity();
		} catch (RestClientResponseException exception) {
			throw new TossPaymentException("결제 취소에 실패했습니다. (" + exception.getStatusCode() + ")");
		}
	}
}
