package com.boot.compick.member.dto;
import com.boot.compick.member.entity.VerificationPurpose; import jakarta.validation.constraints.*;
public record EmailVerificationRequest(@NotBlank @Email String email,@NotNull VerificationPurpose purpose){}

