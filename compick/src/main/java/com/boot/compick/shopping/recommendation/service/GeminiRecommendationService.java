package com.boot.compick.shopping.recommendation.service;

import com.boot.compick.shopping.recommendation.dto.AiRecommendationResponse;
import com.boot.compick.shopping.recommendation.dto.AiProductCatalog;
import com.boot.compick.shopping.recommendation.dto.AiQuoteItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.Content;
import com.google.genai.types.File;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.UploadFileConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class GeminiRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(GeminiRecommendationService.class);
    private static final int MAX_RETRY = 5;
    private static final long RETRY_DELAY_MILLIS = 10_000L;

    private final String apiKey;
    private final String model;
    private final Resource productResource;
    private final String systemPrompt;
    private final ObjectMapper objectMapper;
    private final AiQuoteParser quoteParser;

    private Client client;
    private volatile File productFile;
    private volatile Map<String, AiQuoteItem> productsByTemporaryId = Map.of();

    public GeminiRecommendationService(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:models/gemini-3.6-flash}") String model,
            @Value("${gemini.product-resource:classpath:shopping/ai/compick-products.csv}") Resource productResource,
            @Value("${gemini.system-prompt-resource:classpath:shopping/ai/system-prompt.txt}") Resource promptResource,
            ObjectMapper objectMapper,
            AiQuoteParser quoteParser) throws IOException {
        this.apiKey = apiKey;
        this.model = model;
        this.productResource = productResource;
        this.systemPrompt = promptResource.getContentAsString(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
        this.quoteParser = quoteParser;
    }

    /** geminiFileUpload.py와 동일하게 서버가 준비되는 즉시 CSV를 업로드한다. */
    @EventListener(ApplicationReadyEvent.class)
    public void uploadProductFileOnStartup() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY가 없어 시작 시 상품 파일 업로드를 건너뜁니다.");
            return;
        }

        try (var input = productResource.getInputStream()) {
            String sourceCsv = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            AiProductCatalog catalog = quoteParser.createCatalog(sourceCsv);
            File uploadedFile = getClient().files.upload(
                    catalog.uploadData(),
                    UploadFileConfig.builder().mimeType("text/csv").build());

            String uploadedFileName = uploadedFile.name()
                    .orElseThrow(() -> new IllegalStateException("업로드한 상품 파일 이름을 확인할 수 없습니다."));

            // compick.py의 client.files.get(name=filename) 과정과 동일하다.
            productFile = getClient().files.get(uploadedFileName, null);
            productsByTemporaryId = catalog.productsById();
            log.info(
                    "Gemini 상품 파일 업로드 및 조회 완료: {} (응답 MIME: {}, 요청 MIME: text/csv)",
                    uploadedFileName,
                    productFile.mimeType().orElse("없음"));
        } catch (IOException | RuntimeException e) {
            log.error("서버 시작 중 Gemini 상품 파일 업로드에 실패했습니다.", e);
        }
    }

    public AiRecommendationResponse recommend(String userInput) {
        validateReady();

        File currentFile = productFile;
        Content contents = Content.fromParts(
                Part.fromUri(
                        currentFile.uri().orElseThrow(
                                () -> new IllegalStateException("업로드한 상품 파일 URI를 확인할 수 없습니다.")),
                        "text/csv"),
                Part.fromText(systemPrompt + userInput));

        return parseResponse(generateContentWithRetry(contents).text());
    }

    public List<AiQuoteItem> resolveProducts(List<String> temporaryIds) {
        if (temporaryIds == null || temporaryIds.isEmpty()) {
            return List.of();
        }
        return temporaryIds.stream()
                .map(productsByTemporaryId::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private GenerateContentResponse generateContentWithRetry(Content contents) {
        for (int retry = 0; retry < MAX_RETRY; retry++) {
            try {
                // Python 코드처럼 별도의 응답 형식 설정 없이 호출한다.
                return getClient().models.generateContent(model, contents, null);
            } catch (ApiException e) {
                if (!isServerError(e) || retry == MAX_RETRY - 1) {
                    log.error("Gemini 추천 요청 실패: HTTP {} {} - {}", e.code(), e.status(), e.message(), e);
                    throw new IllegalStateException(toUserMessage(e), e);
                }

                log.warn("Gemini 서버 과부하 ({}/{}) - 10초 후 재시도", retry + 1, MAX_RETRY);
                waitBeforeRetry();
            }
        }

        throw new IllegalStateException("AI 추천 서버가 응답하지 않습니다. 잠시 후 다시 시도해 주세요.");
    }

    private boolean isServerError(ApiException exception) {
        return exception.code() >= 500 && exception.code() < 600;
    }

    private AiRecommendationResponse parseResponse(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            throw new IllegalStateException("AI 추천 결과가 비어 있습니다.");
        }

        try {
            AiRecommendationResponse response =
                    objectMapper.readValue(stripMarkdownCodeFence(responseText), AiRecommendationResponse.class);
            if (response.productIds() == null || response.productIds().isEmpty()) {
                throw new IllegalStateException("AI 추천 결과에 상품 식별값이 없습니다.");
            }
            return response;
        } catch (JsonProcessingException e) {
            log.error("Gemini 추천 결과 JSON 해석 실패: {}", responseText, e);
            throw new IllegalStateException("AI 추천 결과를 해석하지 못했습니다.", e);
        }
    }

    private String stripMarkdownCodeFence(String text) {
        return text.trim()
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "");
    }

    private String toUserMessage(ApiException exception) {
        return switch (exception.code()) {
            case 400 -> "Gemini 요청 형식이 올바르지 않습니다.";
            case 401, 403 -> "Gemini API 키 또는 사용 권한을 확인해 주세요.";
            case 404 -> "Gemini 모델 또는 업로드한 상품 파일을 찾을 수 없습니다.";
            case 429 -> "AI 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.";
            default -> "AI 추천 서버가 응답하지 않습니다. 잠시 후 다시 시도해 주세요.";
        };
    }

    private Client getClient() {
        if (client == null) {
            client = Client.builder().apiKey(apiKey).build();
        }
        return client;
    }

    private void validateReady() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY 환경변수를 설정해 주세요.");
        }
        if (productFile == null) {
            throw new IllegalStateException("상품 데이터 파일이 업로드되지 않았습니다. 서버 로그를 확인해 주세요.");
        }
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI 추천 요청이 중단되었습니다.", e);
        }
    }

}
