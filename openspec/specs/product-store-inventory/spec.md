# product-store-inventory Specification

## Purpose

상품별 매장 정보와 재고 수량, 픽업 지원 여부 및 공유 찜 상태를 조회한다. 품절 제외 조건과 입력·저장소 오류를 일관되게 처리하며, 변경된 ERD의 필수 데이터 제약을 적용한다.

## Requirements

### Requirement: 상품별 매장 재고 조회
시스템은 SHALL [API 계약](../../../docs/api/api-contract.md)의 상품별 매장 재고 조회 응답을 제공한다.

#### Scenario: 기본 품절 제외
- **WHEN** soldOut을 생략하거나 true로 조회한다
- **THEN** 해당 상품의 재고가 1 이상인 매장만 반환한다

#### Scenario: 품절 필터 해제
- **WHEN** soldOut=false로 조회한다
- **THEN** 해당 상품의 재고 수량이 0인 매장도 반환한다

#### Scenario: 빈 결과
- **WHEN** 상품은 존재하지만 조건에 맞는 재고 관계가 없다
- **THEN** 성공 응답과 상단 상품 정보 및 빈 매장 배열을 반환한다

#### Scenario: 상단 상품 정보
- **WHEN** 존재하는 상품의 매장 재고를 조회한다
- **THEN** 상품 목록과 같은 필드·표시 형식으로 선택한 상품 정보를 product에 포함하며 productId와 일치하는 상품을 반환한다

#### Scenario: 공유 찜 반영
- **WHEN** 매장 찜을 변경한 후 재고를 조회한다
- **THEN** bookmarked에 저장된 공유 찜 상태를 반환한다

#### Scenario: 영업 상태와 픽업·품절 구분
- **WHEN** 영업 상태 CLOSED, 재고 0, 픽업 미지원 매장을 soldOut=false로 조회한다
- **THEN** businessStatus=CLOSED, stockQuantity=0, pickupAvailable=false를 서로 독립적으로 반환한다

### Requirement: 거리 단위 표시
시스템은 SHALL 원본 distanceMeters를 유지하고 화면 표시용 distance를 m 또는 km 문자열로 제공한다.

#### Scenario: 1000미터 미만
- **WHEN** 거리가 999미터 이하이다
- **THEN** 미터 정수에 m을 붙여 반환한다

#### Scenario: 킬로미터 전환
- **WHEN** 거리가 1000미터 이상이다
- **THEN** km로 변환해 소수 첫째 자리까지 반올림하고 불필요한 소수 0을 제거한다

### Requirement: 조회 오류 구분
시스템은 SHALL 계약의 입력 오류·상품 없음·저장소 장애·서버 오류를 구분해 공통 실패 형식으로 반환한다.

#### Scenario: 잘못된 입력
- **WHEN** 상품 ID가 양의 int64 범위를 벗어나거나 soldOut이 true 또는 false가 아니다
- **THEN** 계약의 400 오류를 반환한다

#### Scenario: 상품 없음
- **WHEN** 유효한 ID에 해당하는 상품이 없다
- **THEN** 404 오류를 반환한다

#### Scenario: 저장소 장애
- **WHEN** 연결 실패 또는 타임아웃으로 조회할 수 없다
- **THEN** 내부 정보를 노출하지 않는 503 오류를 반환한다

### Requirement: ERD의 필수 데이터 보존
시스템은 SHALL 매장 이미지·재고 수량에 NOT NULL을 적용하고 기존 상품의 INT 가격·리뷰 수와 필수 이미지 정책을 보존한다.

#### Scenario: 정상 데이터 업그레이드
- **WHEN** 기존 데이터가 필수값을 만족한다
- **THEN** 기존 값과 관계를 보존하면서 스키마를 변경한다

#### Scenario: 기존 NULL 데이터
- **WHEN** 매장 이미지 또는 재고 수량에 NULL이 있다
- **THEN** 임의 보정 없이 migration을 실패시킨다
