package com.boot.compick.shopping.recommendation.service;

import com.boot.compick.shopping.recommendation.dto.AiProductCatalog;
import com.boot.compick.shopping.recommendation.dto.AiQuoteItem;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AiQuoteParser {

    public AiProductCatalog createCatalog(String sourceCsv) {
        List<List<String>> rows = parseRows(sourceCsv);
        if (rows.size() < 2) {
            throw new IllegalStateException("AI 상품 데이터가 비어 있습니다.");
        }
        rows.getFirst().set(0, rows.getFirst().getFirst().replace("﻿", ""));

        Map<String, AiQuoteItem> productsById = new LinkedHashMap<>();
        StringBuilder uploadCsv = new StringBuilder("ai_product_id,")
                .append(toCsvRow(rows.getFirst()))
                .append('\n');

        int sequence = 1;
        for (int index = 1; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            if (row.size() < 4 || row.getFirst().isBlank()) {
                continue;
            }

            String temporaryId = "AI-%06d".formatted(sequence++);
            String category = row.get(0).trim();
            productsById.put(temporaryId, new AiQuoteItem(
                    categoryLabel(category),
                    row.get(2).trim(),
                    parsePrice(row.get(3))));
            uploadCsv.append(temporaryId)
                    .append(',')
                    .append(toCsvRow(row))
                    .append('\n');
        }

        return new AiProductCatalog(
                uploadCsv.toString().getBytes(StandardCharsets.UTF_8),
                Map.copyOf(productsById));
    }

    private String toCsvRow(List<String> fields) {
        return fields.stream()
                .map(this::escapeCsvField)
                .collect(Collectors.joining(","));
    }

    private String escapeCsvField(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return '"' + field.replace("\"", "\"\"") + '"';
        }
        return field;
    }

    private List<List<String>> parseRows(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < csv.length(); index++) {
            char current = csv.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < csv.length() && csv.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                row.add(field.toString());
                field.setLength(0);
            } else if ((current == '\n' || current == '\r') && !quoted) {
                if (current == '\r' && index + 1 < csv.length() && csv.charAt(index + 1) == '\n') {
                    index++;
                }
                row.add(field.toString());
                field.setLength(0);
                if (!row.stream().allMatch(String::isBlank)) {
                    rows.add(new ArrayList<>(row));
                }
                row.clear();
            } else {
                field.append(current);
            }
        }

        row.add(field.toString());
        if (!row.stream().allMatch(String::isBlank)) {
            rows.add(row);
        }
        return rows;
    }

    private long parsePrice(String price) {
        try {
            return Long.parseLong(price.trim());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    /**
     * 부품쇼핑/견적구매 화면과 카테고리 표시를 통일한다 (CategoryDisplay.CATEGORY_TABS 참고).
     * 원본(시현)에는 STORAGE 계열이 "SSD / HDD"로 남아있었는데, 이 프로젝트는 이미 "저장장치"로
     * 통일했고 system-prompt.txt도 "저장장치"를 기준으로 작성돼 있어 여기만 표시가 어긋나 있었다.
     */
    private String categoryLabel(String category) {
        return switch (category.toUpperCase(Locale.ROOT)) {
            case "CPU" -> "CPU";
            case "COOLER", "CPU_COOLER" -> "CPU 쿨러";
            case "MOTHERBOARD", "MAINBOARD" -> "메인보드";
            case "MEMORY", "RAM" -> "RAM";
            case "GPU", "VIDEO_CARD" -> "그래픽카드";
            case "SSD", "HDD", "STORAGE" -> "저장장치";
            case "POWER_SUPPLY", "PSU" -> "파워";
            case "CASE" -> "케이스";
            default -> category;
        };
    }
}
