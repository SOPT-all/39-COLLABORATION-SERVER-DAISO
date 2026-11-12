# API Contract

> 문서 유형: API Contract
> 상태: Draft
> 정본 위치: `docs/api/api-contract.md`
> 기준: [팀 API 명세서](https://app.notion.com/p/API-65614af65e0683fc8032815fa74f5650?source=copy_link) — 상세 API 확인 및 팀 확정 정책 반영. 구현 완료를 뜻하지 않음
> 버전: v0.1
> 최종 수정: 2026-11-13

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

| Method | Path | 설명 | 담당자 | 코드 구현 상태 |
| --- | --- | --- | --- | --- |
| `PATCH` | `/stores/{storeId}/favorite` | [관심매장 찜](#4-관심매장-찜-설정-및-해제) | 김가영 | 구현됨 |
| `GET` | `/products/{productId}/stores` | 상품 재고 조회 | 김가영 | 미구현 |
| `GET` | `/products` | [상품 목록 조회](#6-상품-목록-조회) | 노성빈 | 구현됨 |
| `PATCH` | `/products/{productId}/like` | [상품 좋아요](#5-상품-좋아요-설정-및-해제) | 노성빈 | 구현됨 |

기능 요구사항·시나리오는 [OpenSpec](../../openspec/AGENTS.md)에서 관리한다. 이 문서의 Draft API 목록은 구현 완료된 기능 명세가 아니다.

## 4. 관심매장 찜 설정 및 해제

- 담당자: 김가영
- `PATCH /api/v1/stores/{storeId}/favorite`
- 인증·사용자 ID 없음. 매장별 공유 상태이며 같은 값의 반복 요청은 상태를 유지한다.
- 요청 헤더: `Content-Type: application/json`
- 경로 `storeId`: 필수 BIGINT. `1` 이상 `9223372036854775807` 이하 정수.
- JSON 객체의 `favorite`: 필수 boolean. `true`는 설정, `false`는 해제. 누락과 null은 허용하지 않는다.
- 동작 시나리오: [store-favorite 현재 명세](../../openspec/specs/store-favorite/spec.md).

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

## 5. 상품 좋아요 설정 및 해제

노션의 [상품 좋아요 계약](https://app.notion.com/p/41114af65e068099b5d9fb71ea42fbd9)을 반영한 구현이다. [현재 명세](../../openspec/specs/product-like/spec.md)에서 동작 시나리오를, [보관된 변경 제안](../../openspec/changes/archive/2026-11-13-product-like/proposal.md)에서 구현 범위를 확인한다.

- 담당자: 노성빈
- `PATCH /api/v1/products/{productId}/like`
- 요청 헤더: `Content-Type: application/json`
- `productId`: 필수 정수. `1` 이상 `9223372036854775807` 이하.
- JSON 객체의 `liked`: 필수 boolean. 누락·null·문자열·숫자·객체·배열은 허용하지 않는다.
- 인증·공유 상태·값 저장 기준은 [공통 정책](#24-상품-좋아요매장-찜)을 따른다.

요청 예시:

```json
{"liked": true}
```

성공 응답: `200 OK`. `data.productId`는 BIGINT, `data.liked`는 저장된 boolean 상태다.

```json
{
  "code": "SUCCESS",
  "message": "상품 좋아요 상태를 저장했습니다.",
  "data": {"productId": 1001, "liked": true}
}
```

| 상황 | HTTP 상태 | code | message |
| --- | --- | --- | --- |
| 상품 ID 형식 오류 또는 양의 int64 범위 밖 | 400 | INVALID_PRODUCT_ID | 유효하지 않은 상품 ID입니다. |
| 본문·liked 누락, null, 불리언이 아닌 값, 잘못된 JSON 또는 객체가 아닌 본문 | 400 | INVALID_REQUEST | 요청 본문이 올바르지 않습니다. |
| 유효한 ID에 해당하는 상품 없음 | 404 | PRODUCT_NOT_FOUND | 상품을 찾을 수 없습니다. |
| 서버 내부 오류 | 500 | INTERNAL_SERVER_ERROR | 서버 내부 오류가 발생했습니다. |

응답 형식과 405·415 처리는 [공통 응답](#23-공통-응답)을 따른다. 본문 오류는 매장 찜 전용 오류 코드와 구분하여 위 계약을 적용한다.

## 6. 상품 목록 조회

[노션 계약](https://app.notion.com/p/41114af65e068053bb99e20f1f55899b)과 사용자 확인을 반영한 구현이다. [보관된 변경 제안](../../openspec/changes/archive/2026-11-13-product-list/proposal.md)에서 범위를, [현재 명세](../../openspec/specs/product-list/spec.md)에서 동작을 확인한다.

- `GET /api/v1/products`, 담당자: 노성빈. 인증·사용자 ID·요청 본문은 사용하지 않는다.
- 선택 쿼리 `keyword`(String)는 앞뒤 공백을 제거하고 상품명 부분 일치 또는 품번 정확 일치로 검색한다. 숫자로만 된 검색어에도 두 조건을 모두 적용하며 품번은 문자열로 비교한다. 생략·빈 문자열·공백이면 전체 조회한다. 앱의 빈 검색어 제출 무시와 API의 전체 조회는 구분한다.
- 검색어의 `%`, `_`는 검색 범위를 넓히는 와일드카드가 아닌 일반 문자로 취급한다. 영문 대소문자 비교는 DB의 collation을 따르며 별도 검색 규칙을 추가하지 않는다. URL 예약 문자는 클라이언트가 인코딩해 전달한다.
- 페이지네이션 없이 조건에 맞는 모든 상품을 ID 오름차순으로 반환한다. 별도 정렬·필터 옵션을 추가하지 않는다.
- 성공은 `200`, 메시지는 `상품 목록을 조회했습니다.`이며 `data.products` 배열에 productId(BIGINT), productNumber·name·price·imageUrl·reviewCount(String), liked(boolean)를 담는다. 결과가 없으면 빈 배열이다.
- 응답 항목은 null을 허용하지 않는다. imageUrl에는 실제 상품 이미지 또는 사용자가 준비한 “이미지 없음” 이미지의 URL을 저장하고 그대로 반환한다. API에서 임의의 기본 URL을 생성하거나 null·빈 문자열로 대체하지 않는다. liked는 [공통 정책](#24-상품-좋아요매장-찜)에 따른 저장값이다.
- price는 서버에서 천 단위 쉼표와 `원`을 포함해 반환한다. reviewCount는 9,999 이하에서 쉼표를 적용하고 9,999 초과일 때만 `9999+`로 반환한다. `개`는 붙이지 않는다.
- NEW 배지는 프론트에서 정적으로 표시한다. 별점은 제외하며 두 항목의 서버 필드를 추가하지 않는다.

가격·리뷰 수의 DB 타입은 INT이며 위 문자열은 API 응답의 표시 형식이다. 상품 좋아요의 Product·Repository를 재사용한다. V1은 보존하고 V2와 Product 매핑으로 상품 이미지 NOT NULL을 적용한다. 기존 데이터의 URL 준비와 전환 절차는 [개발 가이드](../development.md#스키마-변경-절차)를 따른다.

요청 예시:

```http
GET /api/v1/products
GET /api/v1/products?keyword=마스크팩
```

| 값 | price 응답 | reviewCount 응답 |
| --- | --- | --- |
| 0 | `"0원"` | `"0"` |
| 1000 | `"1,000원"` | `"1,000"` |
| 9999 | `"9,999원"` | `"9,999"` |
| 10000 | `"10,000원"` | `"9999+"` |

```json
{
  "code": "SUCCESS",
  "message": "상품 목록을 조회했습니다.",
  "data": {
    "products": [{
      "productId": 1001,
      "productNumber": "1023456",
      "name": "수분 마스크팩",
      "price": "1,000원",
      "imageUrl": "https://example.com/product.png",
      "reviewCount": "12",
      "liked": false
    }]
  }
}
```

실패는 공통 오류 응답을 사용한다. 서버 내부 오류는 `500`, `INTERNAL_SERVER_ERROR`, `서버 내부 오류가 발생했습니다.`다.

## 문서 작성 기준

- 각 API에 메서드·경로, 인증·헤더, 입력 타입·필수값·제약·기본값, 요청·응답 예시, 상태 코드·오류 조건을 작성한다. 합의 전인 값은 미정으로 표시한다.
- 기능 동작·시나리오는 OpenSpec에 두고 계약에서는 해당 명세를 연결한다. 필드 표와 응답 예시는 이 문서가 원본이다.
- 내용이 커져 독립적인 독자·용도가 생길 때 기능별 문서로 나누고 이 문서에서 연결한다.
