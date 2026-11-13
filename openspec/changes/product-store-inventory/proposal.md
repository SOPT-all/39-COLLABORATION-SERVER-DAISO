# Proposal

## Why

상품별 매장 재고를 조회하는 API가 없어 매장 정보와 품절 여부, 픽업 지원·공유 찜 상태를 제공할 수 없다. 사용자 제공 이슈 #8 및 상세 계약과 변경 ERD를 기준으로 구현한다.

## What Changes

- 상품별 매장 재고 조회와 기본 품절 제외 필터를 제공한다.
- 입력 오류, 상품 없음, 저장소 장애를 계약에 맞는 응답으로 구분한다.
- 기존 Product의 INT 가격·리뷰 수와 필수 이미지 정책을 유지하며 매장 이미지와 재고 수량을 NOT NULL로 변경한다. 기존 NULL이 있으면 migration을 실패시키며 임의로 보정하지 않는다.

## Capabilities

### New Capabilities

- `product-store-inventory`: 상품별 매장 재고 조회, 품절 필터 및 조회 오류 처리.

### Modified Capabilities

없음.

## Impact

완성된 Product에 연결하는 StoreInventory 엔티티와 조회 API, V3 migration, Store 매핑 및 관련 테스트를 변경한다. HTTP 계약의 원본은 docs/api/api-contract.md이고 선택 근거는 ADR 0002와 개발 가이드를 따른다.
