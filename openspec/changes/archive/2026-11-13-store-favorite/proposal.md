# Proposal

## Why

관심매장 찜의 사용자 제공 계약을 실제 JPA 저장 동작으로 제공한다. 현재는 애플리케이션 진입점과 V1 스키마만 있고 API와 엔티티는 없다.

## What Changes

- 매장별 공유 찜 상태 설정·해제 및 동일 값 반복 요청을 지원한다.
- 명세의 입력 검증과 공통 성공·실패 응답을 구현한다.
- 기존 store 스키마에 매핑하고, HTTP 계약 및 별도 MySQL 검증을 추가한다.

## Capabilities

### New Capabilities

- `store-favorite`: 매장별 공유 찜 상태의 저장과 잘못된 요청의 거부.

### Modified Capabilities

없음.

## Impact

store Controller·Service·Repository·Entity와 필요한 global 응답·예외 처리 코드를 추가한다. 기존 V1 SQL과 DB 구조는 유지한다. HTTP 필드·오류의 원본은 docs/api/api-contract.md다.
