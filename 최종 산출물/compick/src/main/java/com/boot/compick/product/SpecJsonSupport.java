package com.boot.compick.product;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * PRODUCT.spec_json(자유 형식 JSON)에서 특정 키의 정수값을 읽어온다.
 * "Memory Slots": "4"처럼 값이 문자열에 섞여 있어도 첫 숫자만 뽑아 쓴다.
 */
public final class SpecJsonSupport {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Pattern DIGITS = Pattern.compile("\\d+");

	private SpecJsonSupport() {
	}

	public static Map<String, String> readAll(String specJson) {
		Map<String, String> specs = new LinkedHashMap<>();
		if (specJson == null || specJson.isBlank()) {
			return specs;
		}
		try {
			JsonNode node = MAPPER.readTree(specJson);
			node.fields().forEachRemaining(entry -> {
				String value = entry.getValue().asText();
				if (!value.isBlank()) {
					specs.put(entry.getKey(), value);
				}
			});
		} catch (Exception ignored) {
			// 파싱 실패 시 빈 사양 목록으로 처리한다
		}
		return specs;
	}

	public static Integer readInt(String specJson, String key) {
		if (specJson == null || specJson.isBlank()) {
			return null;
		}
		try {
			JsonNode node = MAPPER.readTree(specJson).get(key);
			if (node == null || node.isNull()) {
				return null;
			}
			Matcher matcher = DIGITS.matcher(node.asText());
			return matcher.find() ? Integer.valueOf(matcher.group()) : null;
		} catch (Exception ignored) {
			return null;
		}
	}
}
