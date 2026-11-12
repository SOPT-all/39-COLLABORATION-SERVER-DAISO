# Proposal

## Why

상품 목록 조회에 앞서 상품별 공유 좋아요 저장을 구현한다. 제안 시점의 머지된 dev와 작업 브랜치에는 매장 찜과 공통 응답·예외 처리, V1 product 테이블이 있지만 상품 Java 코드는 없었다. 이 기준에서 필요한 상품 계층을 추가한다.

## What Changes

- 기존 상품 좋아요 계약에 따라 요청한 상태의 저장·해제·반복 요청을 구현한다.
- V1 product 테이블에 매핑하는 Product·Repository와 좋아요 Service·Controller·DTO를 추가하고 기존 공통 예외 처리를 재사용한다.
- HTTP 계약·문서 정합성을 확인하고 HTTP 및 별도 MySQL 검증을 추가한다.

## Capabilities

### New Capabilities

- `product-like`: 상품별 공유 좋아요 저장과 잘못된 요청의 거부.

### Modified Capabilities

없음.

## Impact

- 이슈: https://github.com/SOPT-all/39-COLLABORATION-SERVER-DAISO/issues/9
- product 계층과 global.exception.ErrorType의 상품 오류 항목이 추가될 예정이다. 기존 매장 API와 V1 SQL은 변경하지 않는다.
- HTTP 계약의 원본은 docs/api/api-contract.md, 현재 개발 규칙은 docs/development.md다.
