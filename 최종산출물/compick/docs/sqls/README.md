# COMPICK Oracle SQL

## 새 데이터베이스

1. `compick_schema_oracle.sql`
2. `compick_products_data.sql`

순서로 실행합니다. 스키마 파일은 테이블, 제약조건, 조회 인덱스와 기본 카테고리를 생성하고 상품 데이터 파일은 CSV 기반 상품을 `MERGE` 방식으로 입력합니다.

## 기존 데이터베이스

`compick_latest_migration.sql`만 실행합니다. 기존 데이터를 삭제하지 않고 현재 엔티티에 필요한 컬럼, 상품 판매 상태 제약조건과 조회 인덱스를 반영합니다. 같은 스크립트를 다시 실행해도 컬럼과 인덱스는 중복 생성되지 않습니다.

`archive` 폴더는 과거 작업 기록이므로 현재 DB 구성에는 사용하지 않습니다.
