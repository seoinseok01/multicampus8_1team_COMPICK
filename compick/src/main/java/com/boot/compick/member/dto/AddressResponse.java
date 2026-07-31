package com.boot.compick.member.dto;

import com.boot.compick.member.entity.Address;

public record AddressResponse(
	Long addressId,
	String addressName,
	String recipientName,
	String phone,
	String zipCode,
	String address1,
	String address2,
	boolean isDefault
) {
	public static AddressResponse from(Address address) {
		return new AddressResponse(
			address.getId(),
			address.getAddressName(),
			address.getRecipientName(),
			address.getRecipientPhone(),
			address.getZipCode(),
			address.getBasicAddress(),
			address.getDetailAddress(),
			address.isDefault()
		);
	}
}
