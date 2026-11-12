# Design

## Context

현재 Store 엔티티와 공통 응답·예외 처리 구현이 없다. [개발 규칙](../../../docs/development.md#계층공통-예외-처리)과 [ADR 0002](../../../docs/adr/0002-manage-schema-with-jpa-and-flyway.md)를 따른다. 동기는 proposal.md를 참고한다.

## Goals / Non-Goals

**Goals:** 기존 V1 테이블에 정확히 매핑하며 입력 타입을 엄격하게 검증한다.

**Non-Goals:** 사용자 관계, 매장 생성 API, 새 DB 컬럼은 추가하지 않는다.

## Decisions

- Controller → Service → JpaRepository 구조와 변경 트랜잭션·엔티티 도메인 메서드를 사용한다. 직접 SQL 갱신 대신 JPA 변경 감지를 사용한다.
- Store에 DynamicUpdate를 적용해 찜 변경이 다른 컬럼을 덮어쓰지 않게 한다. 현재 API에 불필요한 버전 컬럼·잠금은 추가하지 않는다.
- Jackson 3 JsonNode로 JSON 형태와 불리언 타입을 검증한 뒤 Request DTO로 변환한다. Boolean 자동 변환은 문자열·숫자를 허용할 수 있어 사용하지 않는다.
- 공통 응답과 global.exception의 CustomException·ErrorType·GlobalExceptionHandler를 사용한다. 공통·매장 오류를 단일 Enum과 예외로 관리하여 현재 프로젝트 규모에 맞게 구조를 단순화한다.

## Risks / Trade-offs

- 동시 찜 요청은 DB에서 나중에 적용된 변경이 남는다. 사용자별 상태나 요청 시각 순서 보장은 계약 범위가 아니다.
- HTTP 검증만으로 JPA 저장을 보장할 수 없다 → 별도 MySQL에 Flyway 적용 및 validate 기동 후 저장·반복·해제·실패·다른 필드 보존을 검증한다.

## Migration Plan

기존 V1 스키마를 그대로 사용하므로 후속 migration은 필요하지 않다. 공유 SQL을 수정하지 않는다.
