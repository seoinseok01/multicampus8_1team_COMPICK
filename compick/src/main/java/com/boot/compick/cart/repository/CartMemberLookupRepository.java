package com.boot.compick.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class CartMemberLookupRepository {

	@PersistenceContext
	private EntityManager entityManager;

	public Optional<Long> findActiveMemberIdByLoginId(String loginId) {
		@SuppressWarnings("unchecked")
		List<Number> memberIds = entityManager.createNativeQuery("""
			SELECT member_id
			FROM MEMBER
			WHERE login_id = :loginId
			  AND member_status = 'ACTIVE'
			""")
			.setParameter("loginId", loginId)
			.setMaxResults(1)
			.getResultList();

		return memberIds.stream()
			.findFirst()
			.map(Number::longValue);
	}
}
