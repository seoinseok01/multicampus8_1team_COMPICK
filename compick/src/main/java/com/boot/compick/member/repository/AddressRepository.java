package com.boot.compick.member.repository;

import com.boot.compick.member.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findAllByMemberIdOrderByDefaultYnDescIdDesc(Long memberId);
    Optional<Address> findByIdAndMemberId(Long id, Long memberId);
}
