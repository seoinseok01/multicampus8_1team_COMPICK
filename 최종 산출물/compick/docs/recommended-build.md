# 추천 견적(프리셋) 관리 구조

추천 견적(`/preset`, `/preset/{id}`) 화면에 나오는 4종 프리셋(가성비/고성능 게이밍, 사무용, 크리에이터, 입문용 등)이
어떤 구조로 저장되고, 새 프리셋을 어떻게 추가/수정하며, 향후 관리자 페이지를 어떻게 붙일지 정리한 문서다.
**이번 작업에서 관리자 페이지(컨트롤러/화면)는 만들지 않았다** — 관리자 페이지가 붙기 전까지 필요한 서비스
레이어만 미리 만들어 뒀다.

## 데이터 구조: 하드코딩이 아니라 DB 기반

추천 견적은 코드에 하드코딩되어 있지 않다. 일반 사용자 견적과 동일한 `QUOTE` / `QUOTE_ITEM` 테이블을 쓰되
`quote_type = 'PRESET'`으로 구분되는 행일 뿐이다.

```
QUOTE (quote_type = 'PRESET')
├── quote_id              PK
├── member_id             시스템 계정(login_id='system_preset') 소유로 고정
├── quote_name             예: "가성비 게이밍 PC"
├── quote_type             'PRESET'
├── purpose_tag            OFFICE | GAMING | VIDEO_EDIT | BEGINNER (nullable)
├── summary_description    카드/상세 화면에 노출되는 한 줄 설명
└── QUOTE_ITEM (1:N)
    ├── product_id         PRODUCT FK
    └── quantity           보통 1, RAM만 2 이상 가능
```

즉 "추천 견적을 수정한다"는 결국 "이 QUOTE 행의 이름/설명/태그를 바꾸고, QUOTE_ITEM 구성을 바꾼다"는 것과
같다. 이미 관계형 DB로 관리되고 있어서 새로 JSON 파일이나 별도 config를 도입하지 않고, **서비스 레이어만
CRUD가 가능하도록 보강**하는 방향으로 구조를 개선했다.

## 이번에 추가한 것

### 1. `QuoteEntity` (엔티티)

- `createPreset(systemMemberId, quoteName, purposeTag, summaryDescription)`: PRESET 타입 QuoteEntity를 만드는
  팩토리. 기존에는 `createUserQuote()`만 있었다.
- `updateDetails(quoteName, purposeTag, summaryDescription)`: 이름/태그/설명 수정.
- `replaceItems(Map<Long productId, Integer quantity>)`: 기존 QUOTE_ITEM을 모두 지우고 새 구성으로 교체한다.
  `items`가 `orphanRemoval = true`로 매핑돼 있어서 `clear()`만 호출해도 DB에서 기존 행이 삭제된다.

### 2. `PresetUpsertRequest` (DTO, `quote/dto/PresetUpsertRequest.java`)

프리셋 생성/수정 요청의 형태. 향후 관리자 페이지 컨트롤러가 요청 바디를 그대로 이 타입으로 받아서
`PresetAdminService`에 넘기면 된다.

```java
record PresetUpsertRequest(
    String quoteName,
    PurposeTag purposeTag,          // nullable
    String summaryDescription,
    List<PresetItem> items          // record PresetItem(Long productId, int quantity)
)
```

### 3. `PresetAdminService` (서비스, `quote/service/PresetAdminService.java`)

- `Long createPreset(PresetUpsertRequest request)` — 새 프리셋 생성, 생성된 `quoteId` 반환
- `void updatePreset(Long quoteId, PresetUpsertRequest request)` — 기존 프리셋의 이름/설명/구성 교체
- `void deletePreset(Long quoteId)` — 프리셋 삭제

세 메서드 모두 `QuoteSelectionValidator`(사용자 견적 생성 로직과 공유)를 통해 "8개 카테고리를 모두 포함하는지,
RAM이 메인보드 슬롯 수를 넘지 않는지"를 검증한 뒤 저장한다 — 관리자가 실수로 카테고리가 빠진 프리셋을 등록하는
것을 막아준다.

**주의**: 현재 이 서비스를 호출하는 컨트롤러가 없다. 즉 어떤 HTTP 엔드포인트로도 아직 노출되지 않았고, 프리셋을
추가/수정/삭제하는 유일한 방법은 여전히 SQL을 직접 실행하는 것이다(아래 "지금 당장 새 프리셋을 추가하려면"
참고). 관리자 페이지를 만들 때 이 서비스를 그대로 가져다 쓰면 된다.

## 새로운 추천 견적을 추가하는 방법

### 방법 A — 지금 당장 (SQL, 관리자 페이지 없이)

기존과 동일하게 SQL로 직접 넣는다. `system_preset` 계정의 `member_id`를 조회한 뒤 QUOTE 1행 + QUOTE_ITEM
8행(RAM을 여러 개 넣고 싶으면 그 이상)을 넣으면 된다. 과거 마이그레이션 예시는
`compick/src/main/resources/docs/archive/compick_quote_preset_migration.sql`을 참고.

### 방법 B — 관리자 페이지가 생긴 이후 (권장, 코드 재사용)

관리자 페이지를 만들 때는 아래처럼 얇은 `@RestController`만 추가하고 `PresetAdminService`를 호출하면 된다.
검증 로직, 엔티티 생성 로직은 이미 다 있으므로 새로 작성할 코드가 거의 없다.

```java
@RestController
@RequestMapping("/api/admin/presets")
@PreAuthorize("hasRole('ADMIN')") // SecurityConfig의 /admin/** 규칙과 동일한 권한
public class PresetAdminApiController {

    private final PresetAdminService presetAdminService;

    public PresetAdminApiController(PresetAdminService presetAdminService) {
        this.presetAdminService = presetAdminService;
    }

    @PostMapping
    public Long create(@Valid @RequestBody PresetUpsertRequest request) {
        return presetAdminService.createPreset(request);
    }

    @PutMapping("/{quoteId}")
    public void update(@PathVariable Long quoteId, @Valid @RequestBody PresetUpsertRequest request) {
        presetAdminService.updatePreset(quoteId, request);
    }

    @DeleteMapping("/{quoteId}")
    public void delete(@PathVariable Long quoteId) {
        presetAdminService.deletePreset(quoteId);
    }
}
```

프런트엔드(관리자 화면)는 상품 검색 UI로 8개 카테고리 productId를 고르게 한 뒤 `PresetUpsertRequest` 형태로
직렬화해서 위 API에 보내면 된다. `SecurityConfig`에 `/admin/**`은 이미 `ROLE_ADMIN` 필요로 설정돼 있으니
`/api/admin/**`도 같은 패턴으로 추가하면 된다(현재 `/api/admin/**`는 별도 규칙이 없으므로 컨트롤러를 추가할
때 `SecurityConfig`에 규칙도 함께 추가해야 한다).

## 기존 추천 견적을 수정하는 방법

- **방법 A(지금)**: `UPDATE QUOTE SET ...`, 구성 부품 교체는 `DELETE FROM QUOTE_ITEM WHERE quote_id = ...` 후
  재삽입.
- **방법 B(관리자 페이지 이후)**: `PresetAdminService.updatePreset(quoteId, request)` 호출 — 이름/설명/태그
  변경과 구성 부품 전체 교체를 한 번에 처리한다(부분 수정이 아니라 항상 전체 목록을 다시 받아 교체하는 방식이라
  관리자 UI도 "현재 구성을 불러와서 통째로 다시 저장" 흐름으로 만들면 된다).

## 조회 쪽 구조 (참고, 이미 구현되어 있음)

- `GET /preset?purpose=GAMING` → `QuoteService.findPresets(purposeTag)` → `PresetSummaryResponse` 목록
- `GET /preset/{quoteId}` → `QuoteService.findPresetDetail(quoteId)` → `PresetDetailResponse`
- `PurposeTag`(`quote/entity/PurposeTag.java`)에 새 용도를 추가하려면 enum 값 추가 + `preset-list.html`/
  `preset-detail.html`의 `th:switch` 분기와 `header.html`/`preset-list.html`의 용도 탭 링크를 함께 갱신해야
  한다(용도 자체는 데이터가 아니라 코드에 고정된 값이다 — 자유 텍스트 태그가 필요해지면 `purpose_tag`를
  VARCHAR 자유 입력으로 바꾸는 스키마 변경이 필요하다).
