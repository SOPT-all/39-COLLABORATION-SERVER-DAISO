# Design

## Context

기존 Store·공통 예외 및 V1 스키마가 있다. [개발 규칙](../../../docs/development.md)과 [ADR 0002](../../../docs/adr/0002-manage-schema-with-jpa-and-flyway.md)를 따르며, 동기는 proposal.md에 둔다.

## Goals / Non-Goals

**Goals:** 기존 찜 동작을 유지하면서 ERD와 상세 HTTP 계약에 맞는 조회를 제공한다.

**Non-Goals:** 정렬·페이지네이션·재고 변경·사용자별 찜은 추가하지 않는다.

## Decisions

- Product와 StoreInventory를 JPA로 매핑하고 재고의 상품·매장 참조는 LAZY 관계로 둔다.
- Product 존재 확인 후 JPQL 생성자 projection으로 매장·재고를 한 번에 조회한다. 엔티티 순회 방식의 N+1을 피한다.
- Service는 readOnly 트랜잭션을 사용한다. HTTP 입력은 요청 DTO에서 엄격히 검증한다.
- 재고 조회 Controller에 한정한 global advice로 연결·트랜잭션 시작 실패·타임아웃을 503으로 변환한다. 기존 찜 API의 500 계약은 유지한다.
- ERD의 STRING은 VARCHAR(255)로 매핑한다. 이미지·재고 NULL은 사용자 지시에 따라 실패시킨다. V2는 ALTER TABLE만 사용하며 MySQL strict SQL mode에서 기존 NULL이 있으면 해당 ALTER를 실패시키고 자동 보정하지 않는다.

## Risks / Trade-offs

- 기존 NULL은 배포를 중단한다 → 운영자가 실제 값을 보정한 후 재실행한다. 검증은 별도 MySQL에서 수행한다.
- MySQL DDL은 전체 트랜잭션 롤백이 되지 않는다 → 앞선 ALTER는 적용된 채 남을 수 있으므로 변경 전 백업 및 실패 시 스키마 상태를 확인한다.
- 로컬 update가 기존 NULL이나 타입 변환 문제를 해결하지 않는다 → 별도 migration 프로파일로 업그레이드를 검증한다.

## Migration Plan

V1은 유지한다. V2의 ALTER TABLE로 상품 price·review_count를 VARCHAR(255), 상품·매장 이미지와 재고 수량을 NOT NULL로 변경한다. 빈 DB와 V1 대표 데이터의 업그레이드 및 NULL 거부를 검증한다.
