package com.boot.compick.payment.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class TossPaymentService {

	private final String secretKey;
	private final String confirmUrl;
	private final RestClient restClient;

	public TossPaymentService(
		@Value("${toss.payments.secret-key:}") String secretKey,
		@Value("${toss.payments.confirm-url:https://api.tosspayments.com/v1/payments/confirm}") String confirmUrl,
		RestClient.Builder restClientBuilder
	) {
		this.secretKey = secretKey;
		this.confirmUrl = confirmUrl;
		this.restClient = restClientBuilder.build();
	}

	public Map<String, Object> confirm(
		String paymentKey,
		String orderId,
		long amount
	) {
		if (secretKey.isBlank()) {
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
}
