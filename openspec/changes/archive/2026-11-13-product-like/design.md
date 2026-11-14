# Design

## Context

[제안](proposal.md)의 동기를 따른다. 제안 당시 머지된 dev와 작업 브랜치에는 Store와 global 공통 처리, V1 product 테이블만 있고 Product Java 코드는 없었다. V1의 price·review_count는 INT, image_url은 nullable VARCHAR다. 미머지 코드나 예정된 migration을 구현 전제로 사용하지 않는다. [개발 규칙](../../../../docs/development.md#계층공통-예외-처리), [API 계약](../../../../docs/api/api-contract.md#5-상품-좋아요-설정-및-해제), [스키마 결정](../../../../docs/adr/0002-manage-schema-with-jpa-and-flyway.md)을 참고한다.

## Goals / Non-Goals

**Goals:** 기존 product 스키마에 맞는 엔티티와 변경 트랜잭션을 구현하고 매장 API의 공통 처리를 재사용한다.

**Non-Goals:** 상품 목록 조회, 검색, 사용자 관계, 스키마 변경은 포함하지 않는다.

## Decisions

- product 패키지에 Product·ProductRepository·ProductService·ProductController와 Store와 같은 dto 하위 패키지를 추가한다. 다른 브랜치의 병합을 기다리지 않는다.
- Product는 현재 V1에 맞춰 price·reviewCount를 int, imageUrl을 nullable String으로 매핑한다. id·liked 조회와 changeLiked(boolean) 등 이번 동작에 필요한 메서드만 제공한다. 목록의 표시 문자열은 이후 응답 DTO에서 처리하고 DB 타입 변경의 이유로 삼지 않는다.
- 변경 Service는 findById로 상품을 조회하고 changeLiked를 호출한 뒤 productId·liked 응답을 만든다. save 호출을 추가하는 대신 기존 Store와 같은 트랜잭션·JPA 변경 감지 패턴을 따른다.
- 기존 ApiResponse·ErrorResponse·CustomException·ErrorType·GlobalExceptionHandler를 재사용한다. 현재 없는 INVALID_PRODUCT_ID·PRODUCT_NOT_FOUND만 ErrorType에 추가한다. 별도 예외 상속 계층을 만들지 않으며 본문 오류는 기존 INVALID_REQUEST로 처리한다.
- JSON 형태와 불리언 타입은 기존 요청 변환 패턴으로 엄격히 검사한다. 자동 타입 변환으로 문자열·숫자가 허용되지 않도록 한다.
- Product에도 변경 컬럼만 갱신하는 방식을 적용한다. 다른 트랜잭션의 상품 정보 변경을 덮어쓰지 않게 하되 이번 범위에서 버전 컬럼·분산 잠금을 추가하지 않는다.

## Risks / Trade-offs

- 동시 요청의 호출 순서를 보장하지 않는다 → DB에 적용된 최종 값을 사용하고 동시성 제약을 확대하지 않는다.
- HTTP 모의 테스트만으로 실제 저장을 보장하지 못한다 → 별도 MySQL에서 기존 migration과 매핑 검증, 저장·반복·롤백·다른 컬럼 보존을 확인한다.
- 매장 API와 본문 오류 코드가 다르다 → 각 HTTP 계약을 원본으로 유지하고 상품 계약을 테스트로 확인한다.

## Migration Plan

현재 V1을 그대로 사용한다. 좋아요에는 추가 컬럼이나 migration이 필요하지 않다. 공유된 SQL과 다른 담당 API는 변경하지 않으며 새 의존성도 추가하지 않는다.
