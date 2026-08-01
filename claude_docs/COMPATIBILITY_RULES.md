# 견적구매 호환성 검사 규칙 (설계 문서)

이 문서는 견적구매(`/quotes/new`) 화면의 "호환성 검사" 기능을 **나중에** 구현할 때 따를 규칙을 정리한 것이다. 현재 화면에는 이 규칙에 따른 판정 로직이 실제로 연결돼 있지 않고, "검사 기능 준비 중" 문구만 고정으로 표시된다. `PRODUCT` 테이블에 이미 존재하는 평면 컬럼만으로 계산 가능하도록 설계했다.

## 규칙

| # | 규칙 | 관련 컬럼 | 판정 |
|---|------|-----------|------|
| 1 | 소켓 호환 | `CPU.socket_type`, `MAINBOARD.socket_type` | 두 값이 완전히 같아야 함 |
| 2 | 메모리 규격 호환 | `RAM.memory_type`, `MAINBOARD.memory_type` | 두 값이 완전히 같아야 함 |
| 3 | 폼팩터 호환 | `CASE.form_factor`, `MAINBOARD.form_factor` | 단순 동일 비교가 아니라 크기 서열(`E-ATX > ATX > mATX > ITX`) 기준으로 케이스가 보드 크기 이상이어야 함 |
| 4 | 그래픽카드 장착 길이 | `GPU.gpu_length_mm`, `CASE.max_gpu_length_mm` | `gpu_length_mm <= max_gpu_length_mm` |
| 5 | 파워 용량 | `CPU.power_consumption`, `GPU.recommended_power`, `POWER_SUPPLY.power_capacity_watt` | `(power_consumption + recommended_power) * 1.2 <= power_capacity_watt` (20% 여유율) |

## 알려진 한계

- **CPU_COOLER**: 소켓 호환 여부를 저장하는 컬럼이 스키마에 없어 현재 데이터로는 검사할 수 없다. 향후 `spec_json`에 지원 소켓 목록을 추가하거나 컬럼을 신설해야 한다. 그전까지는 항상 "호환"으로 취급하고 검사 대상에서 제외한다.
- 규칙 1·2·4·5는 값이 정확히 일치/부등식만 비교하면 되므로 구현이 쉽지만, 규칙 3(폼팩터 서열)은 `form_factor` 문자열이 자유 텍스트(`"ATX Mid Tower"`, `"Mini ITX"` 등)라 서열 매핑 테이블이 별도로 필요하다.
- 이 5개 규칙을 모두 통과해도 실제 물리적 호환성을 100% 보장하지는 않는다(예: RAM 슬롯 개수, M.2 슬롯 유무 등은 검사 대상 밖).

## 참고: PRESET 시드 데이터는 이미 규칙 1·2·5를 반영함

`docs/compick_quote_preset_migration.sql`의 프리셋 생성 로직은 CPU 소켓에 맞는 메인보드를 고르고, 메인보드 메모리 규격에 맞는 RAM을 고르며, 파워는 `(CPU 소비전력 + GPU 권장 파워) * 1.2` 이상인 것을 선택한다. 그래서 추천 견적(PRESET) 상세 화면은 "호환성 검사 통과"를 표시하지만, 이는 위 규칙이 시드 단계에서 이미 반영됐기 때문이며 실행 중에 검사 로직이 동작하는 것은 아니다. 사용자가 직접 구성하는 견적(USER 타입)에는 이 보장이 없으므로 "검사 기능 준비 중"으로 다르게 표시한다.
