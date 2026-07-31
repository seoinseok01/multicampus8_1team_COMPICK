package com.boot.compick.member.controller;

import com.boot.compick.member.dto.AddressForm;
import com.boot.compick.member.dto.AddressResponse;
import com.boot.compick.member.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressApiController {
    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<AddressResponse> create(Authentication authentication,
                                                  @Valid @RequestBody AddressForm addressForm) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AddressResponse.from(addressService.save(authentication.getName(), null, addressForm)));
    }

    @PutMapping("/{addressId}")
    public AddressResponse update(Authentication authentication, @PathVariable Long addressId,
                                  @Valid @RequestBody AddressForm addressForm) {
        return AddressResponse.from(addressService.save(authentication.getName(), addressId, addressForm));
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
}
