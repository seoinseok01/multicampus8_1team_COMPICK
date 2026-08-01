package com.boot.compick.product;

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
