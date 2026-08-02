package com.boot.compick.payment.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.boot.compick.payment.dto.KakaoApproveResponse;
import com.boot.compick.payment.dto.KakaoReadyResponse;
import com.boot.compick.payment.service.KakaoPayException;

import java.util.Map;

/**
 * 카카오페이 "단건결제" REST API 호출 전담. cid 기본값(TC0ONETIME)은 카카오가 공개한
 * 테스트용 가맹점 코드라서 비밀값이 아니다 - 실제 서비스로 전환할 때만 발급받은 cid로 바꾸면 된다.
 */
@Component
public class KakaoPayClient {

	private final String secretKey;
	private final String cid;
	private final String baseUrl;
	private final RestClient restClient;

	public KakaoPayClient(
		@Value("${kakaopay.secret-key:}") String secretKey,
		@Value("${kakaopay.cid:TC0ONETIME}") String cid,
		@Value("${kakaopay.base-url:https://open-api.kakaopay.com/online/v1/payment}") String baseUrl,
		RestClient.Builder restClientBuilder
	) {
		this.secretKey = secretKey;
		this.cid = cid;
		this.baseUrl = baseUrl;
		this.restClient = restClientBuilder.build();
	}

	public boolean isConfigured() {
		return secretKey != null && !secretKey.isBlank();
	}

	public KakaoReadyResponse ready(
		String partnerOrderId,
		String partnerUserId,
		String itemName,
		int quantity,
		long totalAmount,
		String approvalUrl,
		String cancelUrl,
		String failUrl
	) {
		requireConfigured();
		return post("/ready", Map.of(
			"cid", cid,
			"partner_order_id", partnerOrderId,
			"partner_user_id", partnerUserId,
			"item_name", itemName,
			"quantity", quantity,
			"total_amount", totalAmount,
			"tax_free_amount", 0,
			"approval_url", approvalUrl,
			"cancel_url", cancelUrl,
			"fail_url", failUrl
		), KakaoReadyResponse.class);
	}

	public KakaoApproveResponse approve(
		String tid,
		String partnerOrderId,
		String partnerUserId,
		String pgToken
	) {
		requireConfigured();
		return post("/approve", Map.of(
			"cid", cid,
			"tid", tid,
			"partner_order_id", partnerOrderId,
			"partner_user_id", partnerUserId,
			"pg_token", pgToken
		), KakaoApproveResponse.class);
	}

	public void cancel(String tid, long cancelAmount) {
		requireConfigured();
		post("/cancel", Map.of(
			"cid", cid,
			"tid", tid,
			"cancel_amount", cancelAmount,
			"cancel_tax_free_amount", 0
		), Map.class);
	}

	private <T> T post(String path, Map<String, ?> body, Class<T> responseType) {
		try {
			T response = restClient.post()
				.uri(baseUrl + path)
				.headers(headers -> {
					headers.set("Authorization", "SECRET_KEY " + secretKey);
					headers.setContentType(MediaType.APPLICATION_JSON);
				})
				.body(body)
				.retrieve()
				.body(responseType);
			if (response == null) {
				throw new KakaoPayException("카카오페이 응답이 비어 있습니다.");
			}
			return response;
		} catch (RestClientResponseException exception) {
			throw new KakaoPayException("카카오페이 요청에 실패했습니다. (" + exception.getStatusCode() + ")");
		}
	}

	private void requireConfigured() {
		if (!isConfigured()) {
			throw new KakaoPayException("카카오페이 시크릿 키가 설정되지 않았습니다.");
		}
	}
}
