# Design

## Context

기존 Store·공통 예외, 상품 목록·검색·좋아요 및 V1·V2 스키마를 기반으로 한다. [개발 규칙](../../../../docs/development.md)과 [ADR 0002](../../../../docs/adr/0002-manage-schema-with-jpa-and-flyway.md)를 따르며, 동기는 proposal.md에 둔다.

## Goals / Non-Goals

**Goals:** 기존 찜 동작을 유지하면서 ERD와 상세 HTTP 계약에 맞는 조회를 제공한다.

**Non-Goals:** 정렬·페이지네이션·재고 변경·사용자별 찜은 추가하지 않는다.

## Decisions

- 완성된 Product의 목록·검색·좋아요 기능을 재사용하고 StoreInventory를 매핑한다. 재고의 상품·매장 참조는 LAZY 관계로 둔다.
- Product를 findById로 조회하고 기존 ProductListItemResponse로 변환하여 상단 상품 정보를 함께 반환한다. JPQL 생성자 projection으로 매장·재고를 한 번에 조회해 전체 SELECT 2회를 유지하고 엔티티 순회 방식의 N+1을 피한다. 매장 결과가 비어도 상품 정보를 포함한다.
- Service는 readOnly 트랜잭션을 사용한다. HTTP 입력은 요청 DTO에서 엄격히 검증한다.
- 목록·좋아요와 분리한 재고 조회 Controller에 한정한 global advice로 연결·트랜잭션 시작 실패·타임아웃을 503으로 변환한다. 기존 상품 목록·좋아요 및 매장 찜 API의 500 계약은 유지한다.
- 상품 가격·리뷰 수는 기존 INT를 유지한다. 매장 이미지·재고 NULL은 사용자 지시에 따라 실패시킨다. V3는 ALTER TABLE만 사용하며 MySQL strict SQL mode에서 기존 NULL이 있으면 해당 ALTER를 실패시키고 자동 보정하지 않는다.

- ProductStoreResponse의 표시용 getter에서 거리 문자열을 계산한다. 원본 distanceMeters와 DB 거리·JPQL 생성자는 유지하며 프론트는 distance를 그대로 표시한다. km는 BigDecimal로 소수 첫째 자리 HALF_UP 반올림하여 부동소수 오차를 피한다.

- 영업 상태는 BusinessStatus Enum을 문자열로 매핑한다. JdbcTypeCode(VARCHAR)로 ERD의 VARCHAR(30)을 유지하며 DB ENUM 타입으로 바꾸지 않는다. OPEN·CLOSED는 직접 저장한 상태이며 품절은 stockQuantity=0, 픽업 미지원은 pickupAvailable=false로 구분한다.

## Risks / Trade-offs

- 기존 NULL은 배포를 중단한다 → 운영자가 실제 값을 보정한 후 재실행한다. 검증은 별도 MySQL에서 수행한다.
- MySQL DDL은 전체 트랜잭션 롤백이 되지 않는다 → 앞선 ALTER는 적용된 채 남을 수 있으므로 변경 전 백업 및 실패 시 스키마 상태를 확인한다.
- 로컬 update가 기존 NULL이나 타입 변환 문제를 해결하지 않는다 → 별도 migration 프로파일로 업그레이드를 검증한다.

## Migration Plan

V1·V2와 Product 매핑은 유지한다. V3의 ALTER TABLE로 매장 이미지와 재고 수량만 NOT NULL로 변경한다. 빈 DB와 V2 대표 데이터의 업그레이드 및 NULL 거부를 검증한다.
