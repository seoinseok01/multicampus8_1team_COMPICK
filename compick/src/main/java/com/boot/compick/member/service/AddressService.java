package com.boot.compick.member.service;

import com.boot.compick.member.dto.AddressForm;
import com.boot.compick.member.entity.Address;
import com.boot.compick.member.entity.Member;
import com.boot.compick.member.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {
    private final AddressRepository addressRepository;
    private final MemberService memberService;

    public List<Address> findAll(String loginId) {
        return addressRepository.findAllByMemberIdOrderByDefaultYnDescIdDesc(memberService.findActiveByLoginId(loginId).getId());
    }

    public Address findOwned(String loginId, Long addressId) {
        Long memberId = memberService.findActiveByLoginId(loginId).getId();
        return addressRepository.findByIdAndMemberId(addressId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("배송지를 찾을 수 없습니다."));
    }

    public AddressForm getForm(String loginId, Long addressId) {
        Address address = findOwned(loginId, addressId);
        AddressForm form = new AddressForm();
        form.setAddressName(address.getAddressName());
        form.setRecipientName(address.getRecipientName());
        form.setRecipientPhone(address.getRecipientPhone());
        form.setZipCode(address.getZipCode());
        form.setBasicAddress(address.getBasicAddress());
        form.setDetailAddress(address.getDetailAddress());
        form.setDefaultAddress(address.isDefault());
        return form;
    }

    @Transactional
    public Address save(String loginId, Long addressId, AddressForm form) {
        Member member = memberService.findActiveByLoginId(loginId);
        if (form.isDefaultAddress()) {
            clearDefaults(member.getId(), addressId);
        }
        if (addressId == null) {
            boolean first = addressRepository.findAllByMemberIdOrderByDefaultYnDescIdDesc(member.getId()).isEmpty();
            return addressRepository.save(new Address(member, form.getAddressName(), form.getRecipientName(), form.getRecipientPhone(),
                    form.getZipCode(), form.getBasicAddress(), form.getDetailAddress(), first || form.isDefaultAddress()));
        } else {
            Address address = findOwned(loginId, addressId);
            address.update(form.getAddressName(), form.getRecipientName(), form.getRecipientPhone(), form.getZipCode(),
                    form.getBasicAddress(), form.getDetailAddress(), form.isDefaultAddress());
            return address;
        }
    }

    @Transactional
    public Address setDefault(String loginId, Long addressId) {
        Address address = findOwned(loginId, addressId);
        clearDefaults(address.getMember().getId(), addressId);
        address.makeDefault();
        return address;
    }

    private void clearDefaults(Long memberId, Long exceptId) {
        addressRepository.findAllByMemberIdOrderByDefaultYnDescIdDesc(memberId).stream()
                .filter(a -> exceptId == null || !a.getId().equals(exceptId)).forEach(Address::clearDefault);
    }

    @Transactional
    public void delete(String loginId, Long addressId) {
        Address address = findOwned(loginId, addressId);
        boolean wasDefault = address.isDefault();
        Long memberId = address.getMember().getId();
        addressRepository.delete(address);
        addressRepository.flush();
        if (wasDefault) {
            addressRepository.findAllByMemberIdOrderByDefaultYnDescIdDesc(memberId).stream()
                    .findFirst()
                    .ifPresent(Address::makeDefault);
        }
    }
}
