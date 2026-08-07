package com.boot.compick.member.controller;

import com.boot.compick.member.dto.AddressForm;
import com.boot.compick.member.dto.AddressRequest;
import com.boot.compick.member.dto.AddressResponse;
import com.boot.compick.member.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressApiController {
    private final AddressService addressService;

	@GetMapping
	public List<AddressResponse> list(Authentication authentication) {
		return addressService.findAll(authentication.getName()).stream()
				.map(AddressResponse::from).toList();
	}

    @PostMapping
	public ResponseEntity<AddressResponse> create(Authentication authentication,
											  @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
				.body(AddressResponse.from(addressService.save(authentication.getName(), null, toForm(request))));
    }

    @PutMapping("/{addressId}")
    public AddressResponse update(Authentication authentication, @PathVariable Long addressId,
								  @Valid @RequestBody AddressRequest request) {
		return AddressResponse.from(addressService.save(authentication.getName(), addressId, toForm(request)));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long addressId) {
        addressService.delete(authentication.getName(), addressId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{addressId}/default")
    public AddressResponse setDefault(Authentication authentication, @PathVariable Long addressId) {
        return AddressResponse.from(addressService.setDefault(authentication.getName(), addressId));
    }

	private AddressForm toForm(AddressRequest request) {
		AddressForm form = new AddressForm();
		form.setAddressName(request.normalizedAddressName());
		form.setRecipientName(request.recipientName());
		form.setRecipientPhone(request.phone());
		form.setZipCode(request.zipCode());
		form.setBasicAddress(request.address1());
		form.setDetailAddress(request.address2());
		form.setDefaultAddress(request.isDefault());
		return form;
	}
}
