# Proposal

## Why

프론트엔드가 상품 목록과 검색 결과를 바로 표시할 수 있도록 노성빈 담당 목록 조회 API를 구현한다. 상품 좋아요 브랜치에 준비된 Product 계층을 재사용하고, 확정된 이미지 필수 계약과 현재 nullable 스키마의 차이를 함께 해소한다.

## What Changes

- [상품 목록 계약](../../../../docs/api/api-contract.md#6-상품-목록-조회)에 따라 전체 조회·검색·정렬과 표시용 응답을 추가한다.
- 기존 product 계층에 조회 흐름과 응답 DTO를 추가하고 저장된 좋아요 상태를 반환한다.
- **BREAKING**: 상품 이미지의 null 저장을 허용하지 않도록 후속 migration과 엔티티 매핑을 변경한다. 기존 null 데이터는 실제 이미지 또는 대체 이미지 URL로 채운 뒤 전환하며 V1은 수정하지 않는다.
- HTTP·MySQL 검증과 기존 이미지 누락 테스트 데이터의 정합성을 확인한다. 이번 제안 작성은 구현 완료를 뜻하지 않는다.

## Capabilities

### New Capabilities

- `product-list`: 상품 전체 조회·검색과 화면 표시용 상품 정보 제공.

### Modified Capabilities

없음. 기존 좋아요 저장 동작은 유지하고 목록 조회에서 그 결과를 읽는다.

## Impact

- 이슈: [#12](https://github.com/SOPT-all/39-COLLABORATION-SERVER-DAISO/issues/12). 선행 작업: [좋아요 PR #11](https://github.com/SOPT-all/39-COLLABORATION-SERVER-DAISO/pull/11).
- product Controller·Service·Repository·Entity와 응답 DTO, 후속 migration, 상품 및 migration 테스트가 변경 대상이다.
- 계약은 위 API 문서, 설계·미정 사항은 [design.md](design.md), 구현 작업은 [tasks.md](tasks.md)에서 관리한다.
- 매장·재고 API, 인증, 추가 검색 의존성은 변경하지 않는다. 완료 전에는 현재 specs/에 동기화하거나 archive하지 않는다.
