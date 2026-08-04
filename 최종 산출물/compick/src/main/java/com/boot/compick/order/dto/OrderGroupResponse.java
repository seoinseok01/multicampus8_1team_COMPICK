package com.boot.compick.order.dto;

public record OrderGroupResponse(
	String groupType,
	String groupName,
	String subtitle,
	long groupTotal
) {
}
