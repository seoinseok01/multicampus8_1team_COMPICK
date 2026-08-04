package com.boot.compick.member.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.boot.compick.member.dto.AddressRequest;
import com.boot.compick.member.dto.AddressResponse;
import com.boot.compick.member.entity.Address;
import com.boot.compick.member.entity.Member;
import com.boot.compick.member.repository.AddressRepository;

@Service
@Transactional(readOnly = true)
public class AddressService {

	private final AddressRepository addressRepository;
	private final MemberService memberService;

	public AddressService(
		AddressRepository addressRepository,
		MemberService memberService
	) {
		this.addressRepository = addressRepository;
		this.memberService = memberService;
	}

	public List<AddressResponse> findAll(String loginId) {
		Long memberId = memberService.findActiveByLoginId(loginId).getId();
		return addressRepository
			.findAllByMemberIdOrderByDefaultYnDescIdDesc(memberId)
			.stream()
			.map(AddressResponse::from)
			.toList();
	}

	@Transactional
	public AddressResponse create(String loginId, AddressRequest request) {
		Member member = memberService.findActiveByLoginId(loginId);
		List<Address> addresses =
			addressRepository.findAllByMemberIdOrderByDefaultYnDescIdDesc(member.getId());
		boolean defaultAddress = addresses.isEmpty() || request.isDefault();

		if (defaultAddress) {
			addresses.forEach(Address::clearDefault);
		}

		Address address = addressRepository.save(new Address(
			member,
			request.normalizedAddressName(),
			request.recipientName().trim(),
			request.phone().trim(),
			request.zipCode().trim(),
			request.address1().trim(),
			nullableTrim(request.address2()),
			defaultAddress
		));
		return AddressResponse.from(address);
	}

	@Transactional
	public AddressResponse update(
		String loginId,
		Long addressId,
		AddressRequest request
	) {
		Address address = findOwned(loginId, addressId);
		boolean keepDefault = address.isDefault() && !request.isDefault();
		if (request.isDefault()) {
			clearDefaults(address.getMember().getId(), addressId);
		}
		address.update(
			request.normalizedAddressName(),
			request.recipientName().trim(),
			request.phone().trim(),
			request.zipCode().trim(),
			request.address1().trim(),
			nullableTrim(request.address2()),
			request.isDefault() || keepDefault
		);
		return AddressResponse.from(address);
	}

	@Transactional
	public AddressResponse setDefault(String loginId, Long addressId) {
		Address address = findOwned(loginId, addressId);
		clearDefaults(address.getMember().getId(), addressId);
		address.makeDefault();
		return AddressResponse.from(address);
	}

	@Transactional
	public void delete(String loginId, Long addressId) {
		Address address = findOwned(loginId, addressId);
		boolean wasDefault = address.isDefault();
		Long memberId = address.getMember().getId();
		addressRepository.delete(address);
		addressRepository.flush();

		if (wasDefault) {
			addressRepository
				.findAllByMemberIdOrderByDefaultYnDescIdDesc(memberId)
				.stream()
				.findFirst()
				.ifPresent(Address::makeDefault);
		}
	}

	private Address findOwned(String loginId, Long addressId) {
		Long memberId = memberService.findActiveByLoginId(loginId).getId();
		return addressRepository.findByIdAndMemberId(addressId, memberId)
			.orElseThrow(() -> new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"배송지를 찾을 수 없습니다."
			));
	}

	private void clearDefaults(Long memberId, Long excludedAddressId) {
		addressRepository
			.findAllByMemberIdOrderByDefaultYnDescIdDesc(memberId)
			.stream()
			.filter(address -> !address.getId().equals(excludedAddressId))
			.forEach(Address::clearDefault);
	}

	private String nullableTrim(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
