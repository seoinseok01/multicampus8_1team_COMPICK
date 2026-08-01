package com.boot.compick.product.repository;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Oracle의 JSON_VALUE는 두 번째 인자(JSON 경로)가 반드시 SQL 리터럴이어야 하고
 * 바인드 변수로 넘기면 ORA-40454가 발생한다. jsonPath는 서버에서 정의한
 * 고정된 값(CategoryDisplay 등)만 들어오므로 SQL에 직접 삽입해도 안전하지만,
 * 방어적으로 형식을 한 번 더 검증한다.
 */
@Repository
public class ProductSpecFacetRepository {

	private static final Pattern SAFE_JSON_PATH = Pattern.compile("^\\$\\.\"[A-Za-z0-9 ]+\"$");

	@PersistenceContext
	private EntityManager entityManager;

	public List<String> findDistinctSpecValues(String categoryName, String jsonPath) {
		if (!SAFE_JSON_PATH.matcher(jsonPath).matches()) {
			throw new IllegalArgumentException("허용되지 않는 JSON 경로입니다: " + jsonPath);
		}

		String escapedPath = jsonPath.replace("'", "''");

		@SuppressWarnings("unchecked")
		List<String> values = entityManager.createNativeQuery("""
			SELECT DISTINCT JSON_VALUE(p.spec_json, '%s')
			FROM PRODUCT p
			JOIN CATEGORY c ON p.category_id = c.category_id
			WHERE c.category_name = :categoryName
			  AND p.sales_status = 'ON_SALE'
			  AND JSON_VALUE(p.spec_json, '%s') IS NOT NULL
			ORDER BY 1
			""".formatted(escapedPath, escapedPath))
			.setParameter("categoryName", categoryName)
			.getResultList();

		return values;
	}
}
