package com.boot.compick.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.compick.cart.entity.CartEntity;

public interface CartRepository extends JpaRepository<CartEntity, Long> {

	Optional<CartEntity> findByMemberId(Long memberId);
}
