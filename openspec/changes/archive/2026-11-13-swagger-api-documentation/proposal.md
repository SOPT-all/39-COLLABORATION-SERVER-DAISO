# Proposal

## Why

팀원이 구현된 API를 한 화면에서 확인하고 직접 호출할 수 있는 문서가 필요하다. 현재의 수동 요청 검증·표시용 응답을 자동 추론만으로 문서화하면 실제 계약과 다른 타입이나 누락된 필드가 나타날 수 있어 이를 보완한다.

## What Changes

- Spring Boot 4.1.2에 호환되는 springdoc으로 OpenAPI 문서와 기본 Swagger UI를 제공한다.
- 로컬과 팀 배포 환경에서 문서 조회·API 실행을 허용하고 상품·매장 태그와 접힌 API 상세로 화면을 정돈한다.
- 구현된 API 4개의 입력·구체적 응답·오류·예시를 [API 계약](../../../../docs/api/api-contract.md)에 맞게 표현한다. 기존 HTTP 동작·직렬화·DB 제약은 유지한다.
- 생성된 명세와 실제 JSON을 대조하고 기존 README·개발 가이드에 접속·유지 관리 방법을 통합한다.

## Capabilities

### New Capabilities

- `api-documentation`: 구현된 HTTP 계약을 조회하고 요청을 실행할 수 있는 API 문서 제공.

### Modified Capabilities

없음. 상품 목록·좋아요·재고 조회·매장 찜의 기존 요구사항은 변경하지 않는다.

## Impact

- [이슈 #14](https://github.com/SOPT-all/39-COLLABORATION-SERVER-DAISO/issues/14), 브랜치 `chore/14-swagger`. 기준은 재고 조회까지 머지된 dev `2eb9b7e`다.
- Gradle 의존성, 공통 OpenAPI 설정 하나, application 설정, Controller 3개와 공개 요청·응답 DTO가 적용 대상이다.
- Entity·Repository·Service·migration과 기존 예외 처리 로직은 변경하지 않는다. 세부 설계는 [design.md](design.md), 구현·검증 순서는 [tasks.md](tasks.md)를 따른다.
- 현재는 구현 전 제안이다. 기존 재고 조회 변경의 보관 등 다른 작업 정리는 포함하지 않는다.
