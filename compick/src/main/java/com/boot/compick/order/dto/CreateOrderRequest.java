package com.boot.compick.order.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {
	private Long addressId;
	private String addressName;
	private String recipientName;
	private String recipientPhone;
	private String zipCode;
	private String basicAddress;
	private String detailAddress;
	private boolean saveAddress;
	private boolean defaultAddress;
	private String deliveryRequest;
}
