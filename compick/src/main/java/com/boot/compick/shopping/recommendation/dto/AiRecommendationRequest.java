package com.boot.compick.shopping.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiRecommendationRequest {

    @NotBlank(message = "원하는 PC의 예산과 용도를 입력해 주세요.")
    @Size(max = 1000, message = "요구사항은 1,000자 이내로 입력해 주세요.")
    private String requirements;

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }
}
