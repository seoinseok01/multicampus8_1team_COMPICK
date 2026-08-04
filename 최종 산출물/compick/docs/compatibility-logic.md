# 견적구매 호환성 검사 로직

`/quotes/new`(견적구매) 화면에서 실제로 동작하는 호환성 검사 기능을 설명한다. 검사는
전적으로 **클라이언트(브라우저)에서** 수행된다 — `compick/src/main/resources/static/js/quote-builder.js`의
`evaluateCompatibility()` 함수가 사용자의 카테고리별 선택(`selected`)이 바뀔 때마다(`renderSummary()`가
호출될 때마다) 다시 계산하고, 결과를 `#quote-summary-panel` 안의 `.compatibility-banner` 영역에 실시간으로 표시한다.
서버는 견적을 장바구니에 담을 때 "8개 카테고리를 모두 선택했는지 / RAM 슬롯 수를 넘지 않는지"만 재검증하며(
`QuoteSelectionValidator`), 소켓·메모리 규격 등 물리적 호환성 자체는 서버에서 다시 검사하지 않는다(아래 "서버 측 검증과의
역할 분담" 참고).

## 언제 검사하는가

- 8개 카테고리를 모두 채우기를 기다리지 않는다. 부품을 하나 선택하거나 바꿀 때마다(RAM 수량 증감 포함)
  `renderSummary()` → `evaluateCompatibility()`가 즉시 다시 실행되어, **그 시점까지 선택된 부품끼리만** 아래
  5개 규칙을 평가한다(각 규칙은 관련된 두 부품이 모두 선택돼 있을 때만 판정하므로 아직 안 고른 카테고리는 자동으로
  건너뛴다).
- 예를 들어 CPU와 메인보드만 고른 상태에서 두 소켓이 다르면, 나머지 6개 카테고리를 고르기 전이라도 즉시
  "⚠ 호환성 문제 1건" 배너가 뜬다. 이후 RAM·GPU·케이스·파워를 고를 때마다 위반 목록이 실시간으로 늘거나 준다.
- 문제가 하나도 없고 아직 8개를 다 고르지 않았다면 "호환성 검사 대기 중"(문제 없음, 계속 골라도 됨)을 표시한다.
  8개를 모두 고르고 문제가 없으면 "✓ 호환성 검사 통과"로 바뀐다.
- 호환성 문제가 하나라도 남아 있으면(8개를 다 골랐어도) "장바구니 담기" 버튼이 비활성화된다 — 문제를 해결해야
  담을 수 있다.

## 검사 순서와 판정 조건

`evaluateCompatibility()` 안에서 아래 순서로 검사하며, 각 규칙은 관련된 두 부품이 모두 선택되어 있고 비교에
필요한 값이 둘 다 존재할 때만 평가한다(값이 없으면 그 규칙은 조용히 건너뛴다 — "알려진 한계" 참고).

| 순서 | 규칙 | 비교 대상 | 데이터 출처 | 판정 조건 |
|---|---|---|---|---|
| 1 | 소켓 호환 | CPU ↔ 메인보드 | `PRODUCT.socket_type` (두 카테고리 모두 평면 컬럼) | `cpu.socketType === board.socketType` |
| 2 | 메모리 규격 호환 | 메인보드 ↔ RAM(선택된 것 전부) | `PRODUCT.memory_type` | 선택된 RAM 각각에 대해 `ram.memoryType === board.memoryType`. RAM을 여러 개 선택했다면 규격이 다른 RAM이 하나라도 있으면 위반으로 보고한다 |
| 3 | 폼팩터 호환 | 메인보드 ↔ 케이스 | `PRODUCT.form_factor` (자유 텍스트) | 아래 "폼팩터 서열" 규칙으로 순위를 매겨 `caseRank >= boardRank`이어야 함 |
| 4 | 그래픽카드 장착 길이 | GPU ↔ 케이스 | `PRODUCT.gpu_length_mm`, `PRODUCT.max_gpu_length_mm` | `gpu.gpuLengthMm <= case.maxGpuLengthMm` |
| 5 | 파워 용량 | (CPU + GPU) ↔ 파워 | `PRODUCT.power_consumption`(CPU), `PRODUCT.recommended_power`(GPU), `PRODUCT.power_capacity_watt`(파워) | `ceil((cpu.powerConsumption + gpu.recommendedPower) * 1.2) <= psu.powerCapacityWatt` (20% 여유율) |

이 5개 컬럼(`gpu_length_mm`, `max_gpu_length_mm`, `power_capacity_watt`)은 스키마(`compick_schema_oracle.sql`)에는
이미 있었지만 이번 작업 전에는 `ProductEntity`에 매핑되어 있지 않았다. 이번에 `ProductEntity`, `ProductListItemResponse`,
`QuoteItemView`에 모두 추가해서 프런트엔드까지 값이 전달되도록 했다.

### 폼팩터 서열 규칙 (규칙 3)

`form_factor` 값은 `"ATX"`, `"Mini ITX"`처럼 자유 텍스트라서 단순 문자열 비교로는 크기 비교를 할 수 없다.
`formFactorRank()`가 대문자로 바꾼 뒤 아래 순서로 키워드를 찾아 순위를 매긴다(먼저 매칭되는 것을 채택하므로
"MicroATX"가 "ATX" 규칙에 앞서 "MICRO" 규칙에 먼저 걸리도록 순서가 중요하다):

1. `E-ATX` / `EATX` 포함 → 5
2. `MICRO` / `MATX` / `M-ATX` 포함 → 3
3. `DTX` 포함 → 2
4. `ITX` 포함 → 1
5. (위에 해당 안 하고) `ATX` 포함 → 4

케이스 순위가 메인보드 순위보다 크거나 같으면 통과(더 큰 케이스에는 작은 보드가 들어간다). 실제 데이터 기준:

- 메인보드 `form_factor`: `Mini ITX`(1), `Mini DTX`(2), `Micro ATX`(3), `ATX`(4)
- 케이스 `form_factor`: `Mini ITX Desktop`(1), `MicroATX Mini/Mid Tower`(3), `ATX Mid/Full Tower`(4)

## 서버 측 검증과의 역할 분담

`QuoteService.buildAndAddToCart()` → `QuoteSelectionValidator.validate()`(
`compick/src/main/java/com/boot/compick/quote/service/QuoteSelectionValidator.java`)는 장바구니에 담기 직전
아래 두 가지만 다시 검증한다(둘 다 물리적 호환성이 아니라 "견적이 완전한가"에 대한 검증):

1. 8개 카테고리를 각각 하나 이상 선택했는가, RAM 외 카테고리는 정확히 1개만 선택했는가
2. 선택한 메인보드의 `spec_json`의 `"Memory Slots"` 값을 넘는 개수의 RAM을 담으려 하지 않는가

소켓/메모리 규격/폼팩터/GPU 길이/파워 용량 같은 물리적 호환성은 서버에서 재검사하지 않는다. 클라이언트 검사를
우회해서 요청을 직접 보내면(예: 다른 소켓의 CPU와 메인보드를 강제로 담기) 서버는 막지 않는다 — 이 부분은 향후
서버 측에도 동일한 규칙을 이식할 수 있도록 `evaluateCompatibility()`의 로직을 그대로 옮길 수 있는 구조로 남겨뒀다.

## 알려진 한계

- **CPU_COOLER 소켓 호환은 검사하지 않는다.** `CPU_COOLER` 카테고리 상품에는 지원 소켓 정보를 저장하는 컬럼이나
  `spec_json` 키가 데이터에 전혀 없다(`spec_json`에는 `Color`/`Radiator`/`Type`만 있다). 따라서 CPU_COOLER는 항상
  "호환 검사 대상 제외"로 취급하며, 실제로 선택한 쿨러가 CPU 소켓을 지원하는지는 사용자가 별도로 확인해야 한다.
  향후 이 검사를 추가하려면 `PRODUCT.spec_json`에 지원 소켓 목록(예: `"Supported Sockets": "AM5, LGA1700"`)을
  채워 넣거나 전용 컬럼을 신설해야 한다.
- 5개 규칙을 모두 통과해도 100% 물리적 호환을 보장하지는 않는다(M.2 슬롯 유무, 라디에이터 장착 공간 등은 검사
  범위 밖).
- RAM 규격(규칙 2)은 규격 문자열이 정확히 같아야 통과한다(`DDR5`끼리는 통과, `DDR4`/`DDR5` 혼용은 위반). 클럭
  속도(예: DDR5-5600 vs DDR5-6000)까지는 비교하지 않는다.

## 관련 파일

- `compick/src/main/resources/static/js/quote-builder.js` — `evaluateCompatibility()`, `formFactorRank()`,
  `renderCompatibilityBanner()`
- `compick/src/main/resources/templates/shopping/quote-new.html` — `.compatibility-banner` 컨테이너
- `compick/src/main/resources/static/css/main.css` — `.compatibility-banner`, `.compatibility-banner.is-pass`,
  `.compatibility-banner.is-fail`
- `compick/src/main/java/com/boot/compick/quote/service/QuoteSelectionValidator.java` — 서버 측 카테고리/RAM
  슬롯 검증
- `compick/src/main/java/com/boot/compick/product/SpecJsonSupport.java` — `spec_json`에서 `Memory Slots` 등
  정수값을 읽는 유틸
