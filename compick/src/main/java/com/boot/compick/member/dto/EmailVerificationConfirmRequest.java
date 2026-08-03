package com.boot.compick.member.dto;
import com.boot.compick.member.entity.VerificationPurpose; import jakarta.validation.constraints.*;
public record EmailVerificationConfirmRequest(@NotBlank @Email String email,@NotNull VerificationPurpose purpose,@NotBlank @Pattern(regexp="\\d{6}") String code){}

