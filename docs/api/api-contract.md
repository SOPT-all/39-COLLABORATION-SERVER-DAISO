# API Contract

> 문서 유형: API Contract
> 상태: Draft
> 정본 위치: `docs/api/api-contract.md`
> 기준: [팀 API 명세서](https://app.notion.com/p/API-65614af65e0683fc8032815fa74f5650?source=copy_link) — 상세 API 확인 및 팀 확정 정책 반영. 구현 완료를 뜻하지 않음
> 버전: v0.1
> 최종 수정: 2026-11-12

## 1. 목적

프론트엔드와 백엔드가 같은 API 계약을 기준으로 개발하기 위한 주소 목록과 공통 응답 기준이다.
사용자가 제공하는 팀 API 명세를 기준으로 작성하며, 회원 인증 API는 구현대상에 없다.

## 2. 공통 규칙

### 2.1 Base URL

| 환경 | Base URL |
| --- | --- |
| local | `http://localhost:8088` |
| dev/staging | 미정 |
| production | 미정 |

공통 API 접두사는 `/api/v1`이다.

### 2.2 인증

회원 인증을 사용하지 않는다.
회원가입·로그인·로그아웃·토큰 재발급 등 회원 인증 API와 JWT 인증 헤더는 계약에 포함하지 않는다.

### 2.3 공통 응답

[팀 공통 응답·예외 처리 컨벤션](https://app.notion.com/p/5c214af65e0682afbfc101d6a2c4a04d)을 반영한 구현 기준이다. 공통 응답·예외 처리 클래스는 global 패키지에 구현되어 있다. 예외 처리 책임은 [개발 가이드](../development.md#계층공통-예외-처리)에서 관리한다.

- 성공 응답은 `ApiResponse<T>`로 `code`, `message`, `data`를 제공한다. 성공 코드는 `SUCCESS`이며 data는 응답 데이터가 없으면 null을 허용한다.
- 실패 응답은 `ErrorResponse`로 `code`, `message`만 제공한다. `data` 필드는 포함하지 않는다.
- 비즈니스 응답 코드는 UPPER_SNAKE_CASE로 작성하고 HTTP 상태와 별도로 제공한다. HTTP 상태도 결과에 맞춰 사용한다.
- `204 No Content`를 선택한 API는 응답 본문을 반환하지 않는다. data가 null인 JSON 응답과 구분한다.

성공 형식 예시:

```json
{"code": "SUCCESS", "message": "요청이 성공적으로 처리되었습니다.", "data": null}
```

실패 형식 예시(404):

```json
{"code": "PRODUCT_NOT_FOUND", "message": "상품을 찾을 수 없습니다."}
```

공통 프로토콜 오류도 `ErrorResponse` 형식으로 반환하며 기존 HTTP 상태와 응답 헤더를 유지한다.

| 상황 | HTTP 상태 | code | message |
| --- | --- | --- | --- |
| 지원하지 않는 HTTP 메서드 | 405 | METHOD_NOT_ALLOWED | 지원하지 않는 HTTP 메서드입니다. |
| 지원하지 않는 Content-Type | 415 | UNSUPPORTED_MEDIA_TYPE | 지원하지 않는 Content-Type입니다. |

### 2.4 상품 좋아요·매장 찜

- 상품 좋아요와 매장 찜은 사용자별로 나누지 않는 공유 상태다. 모든 사용자가 같은 상품·매장의 저장된 상태를 확인한다. 사용자 ID나 사용자별 저장 관계를 추가하지 않는다.
- 상품 좋아요는 `PATCH /api/v1/products/{productId}/like`에 JSON 불리언 `liked`를 전달한다. 매장 찜은 `PATCH /api/v1/stores/{storeId}/favorite`에 JSON 불리언 `favorite`를 전달한다.
- 두 필드는 필수이며 true는 설정, false는 해제다. 기존 상태를 반전하지 않고 요청한 값을 저장한다. 같은 값을 반복 요청하면 최종 상태가 유지된다.
- 상품 찜(`/products/{productId}/save`)이나 별도 매장 북마크 API는 구현 범위에 포함하지 않는다.
- 기준: [상품 좋아요 명세](https://app.notion.com/p/41114af65e068099b5d9fb71ea42fbd9), [매장 찜 명세](https://app.notion.com/p/edd14af65e0683988bf3019f73c5463f).

## 3. API 주소 목록

- 아래 Path는 공통 접두사 `/api/v1`을 제외한 경로다.
- API 주소 수: `4`

| Method | Path | 설명 | 담당자 |
| --- | --- | --- | --- |
| `PATCH` | `/stores/{storeId}/favorite` | 관심매장 찜 | 김가영 |
| `GET` | `/products/{productId}/stores` | 상품 재고 조회 | 김가영 |
| `GET` | `/products` | 상품 목록 조회 | 노성빈 |
| `PATCH` | `/products/{productId}/like` | 상품 좋아요 | 노성빈 |

기능 요구사항·시나리오는 [OpenSpec](../../openspec/AGENTS.md)에서 관리한다. 이 문서의 Draft API 목록은 구현 완료된 기능 명세가 아니다.

## 4. 관심매장 찜 설정 및 해제

- 담당자: 김가영
- `PATCH /api/v1/stores/{storeId}/favorite`
- 인증·사용자 ID 없음. 매장별 공유 상태이며 같은 값의 반복 요청은 상태를 유지한다.
- 요청 헤더: `Content-Type: application/json`
- 경로 `storeId`: 필수 BIGINT. `1` 이상 `9223372036854775807` 이하 정수.
- JSON 객체의 `favorite`: 필수 boolean. `true`는 설정, `false`는 해제. 누락과 null은 허용하지 않는다.
- 동작 시나리오: [store-favorite 변경 명세](../../openspec/changes/store-favorite/specs/store-favorite/spec.md).

요청 예시:

```http
PATCH /api/v1/stores/201/favorite
Content-Type: application/json
```

```json
{"favorite": true}
```

해제 요청은 `{"favorite": false}`다.

성공 응답: `200 OK`. `data.storeId`는 BIGINT, `data.favorite`는 저장된 boolean 상태다.

```json
{
  "code": "SUCCESS",
  "message": "매장 찜 상태를 저장했습니다.",
  "data": {"storeId": 201, "favorite": true}
}
```

실패 응답은 `code`, `message`만 포함한다. 거부한 요청에서는 기존 상태를 변경하지 않는다.

| 상황 | HTTP 상태 | code | message |
| --- | --- | --- | --- |
| 본문 또는 favorite 누락, favorite가 null | 400 | FAVORITE_REQUIRED | 찜 상태는 필수입니다. |
| favorite가 불리언이 아님 | 400 | INVALID_FAVORITE_TYPE | 찜 상태는 true 또는 false여야 합니다. |
| 잘못된 JSON 또는 객체가 아닌 본문 | 400 | INVALID_REQUEST | 요청 본문이 올바르지 않습니다. |
| 매장 ID 형식 오류 또는 양의 int64 범위 밖 | 400 | INVALID_STORE_ID | 유효하지 않은 매장 ID입니다. |
| 유효한 ID에 해당하는 매장 없음 | 404 | STORE_NOT_FOUND | 매장을 찾을 수 없습니다. |
| 서버 내부 오류 | 500 | INTERNAL_SERVER_ERROR | 서버 내부 오류가 발생했습니다. |

```json
{"code": "FAVORITE_REQUIRED", "message": "찜 상태는 필수입니다."}
```

## 5. 상품별 매장 재고 조회

- 담당자: 김가영
- `GET /api/v1/products/{productId}/stores`
- 인증·사용자 ID 및 요청 본문 없음.
- 경로 `productId`: 필수 BIGINT. `1` 이상 `9223372036854775807` 이하 정수.
- 쿼리 `soldOut`: 선택 boolean. 생략 시 `true`. `true`는 재고 1 이상만 조회하고, `false`는 품절 필터를 적용하지 않는다.
- `soldOut`은 소문자 `true`·`false`만 허용하며 빈 값·숫자·중복 입력은 400이다.
- 재고 관계가 없는 매장은 반환하지 않는다. 상품이 존재하되 조건에 맞는 결과가 없으면 200과 `stores: []`를 반환한다.
- 정렬·페이지네이션은 정의하지 않는다.
- 동작 시나리오: [상품 재고 조회 변경 명세](../../openspec/changes/product-store-inventory/specs/product-store-inventory/spec.md).

```http
GET /api/v1/products/1001/stores
GET /api/v1/products/1001/stores?soldOut=false
```

| 응답 필드 | 타입 | 설명 |
| --- | --- | --- |
| data.productId | BIGINT | 상품 ID |
| data.stores | Array<Object> | 조건에 맞는 매장 재고 목록 |
| data.stores[].storeId | BIGINT | 매장 고유 ID |
| data.stores[].storeName | String | 매장명 |
| data.stores[].imageUrl | String | 필수 매장 이미지 URL. 변경 ERD에 따라 null을 허용하지 않음 |
| data.stores[].businessStatus | Enum(String) | 직접 저장한 영업 상태. OPEN: 영업중, CLOSED: 영업 전. 시간 기반 자동 계산 없음 |
| data.stores[].bookmarked | Boolean | 공유 매장 찜 상태. favorite와 같은 값 |
| data.stores[].distanceMeters | BIGINT | 원본 거리. 항상 미터 단위 |
| data.stores[].distance | String | 표시용 거리. 1000m 미만은 m, 이상은 km. 소수 첫째 자리 반올림 및 불필요한 0 제거 |
| data.stores[].stockQuantity | INT | 필수 재고 수량. 0은 품절이며 프론트에서 품절 표시 |
| data.stores[].pickupAvailable | Boolean | true: 픽업 지원, false: 픽업 미지원. 재고 유무와 독립 |

성공 응답: `200 OK`, 아래는 `soldOut=false` 예시다. 사용자 지시에 따라 이미지 NULL 예시 대신 변경 ERD의 NOT NULL을 적용한다.

```json
{
  "code": "SUCCESS",
  "message": "상품의 매장별 재고가 조회되었습니다.",
  "data": {
    "productId": 1001,
    "stores": [
      {
        "storeId": 201,
        "storeName": "강남본점",
        "imageUrl": "https://example.com/stores/201.png",
        "businessStatus": "OPEN",
        "bookmarked": true,
        "distanceMeters": 850,
        "distance": "850m",
        "stockQuantity": 12,
        "pickupAvailable": false
      },
      {
        "storeId": 202,
        "storeName": "역삼점",
        "imageUrl": "https://example.com/stores/202.png",
        "businessStatus": "CLOSED",
        "bookmarked": false,
        "distanceMeters": 1500,
        "distance": "1.5km",
        "stockQuantity": 0,
        "pickupAvailable": true
      }
    ]
  }
}
```

실패 응답은 공통 `code`, `message` 형식을 따른다. 저장소 연결·트랜잭션 시작 실패·조회 타임아웃은 이 조회 API에서 503으로 처리하고, 예상하지 못한 오류는 500으로 처리한다.

| 상황 | HTTP 상태 | code | message |
| --- | --- | --- | --- |
| 상품 ID 형식·범위 오류 | 400 | INVALID_PRODUCT_ID | 유효하지 않은 상품 ID입니다. |
| 잘못된 soldOut | 400 | INVALID_EXCLUDE_SOLD_OUT | 품절 제외 여부는 true 또는 false여야 합니다. |
| 상품 없음 | 404 | PRODUCT_NOT_FOUND | 상품을 찾을 수 없습니다. |
| 서버 내부 오류 | 500 | INTERNAL_SERVER_ERROR | 서버 내부 오류가 발생했습니다. |
| 저장소 장애 | 503 | STORAGE_UNAVAILABLE | 일시적으로 매장별 재고를 조회할 수 없습니다. 잠시 후 다시 시도해 주세요. |

## 문서 작성 기준

- 각 API에 메서드·경로, 인증·헤더, 입력 타입·필수값·제약·기본값, 요청·응답 예시, 상태 코드·오류 조건을 작성한다. 합의 전인 값은 미정으로 표시한다.
- 기능 동작·시나리오는 OpenSpec에 두고 계약에서는 해당 명세를 연결한다. 필드 표와 응답 예시는 이 문서가 원본이다.
- 내용이 커져 독립적인 독자·용도가 생길 때 기능별 문서로 나누고 이 문서에서 연결한다.
