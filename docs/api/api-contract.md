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

[팀 공통 응답·예외 처리 컨벤션](https://app.notion.com/p/5c214af65e0682afbfc101d6a2c4a04d)을 반영한 구현 기준이다. 아직 공통 응답·예외 처리 클래스는 구현되지 않았다. 예외 처리 책임은 [개발 가이드](../development.md#계층공통-예외-처리)에서 관리한다.

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

## 문서 작성 기준

- 각 API에 메서드·경로, 인증·헤더, 입력 타입·필수값·제약·기본값, 요청·응답 예시, 상태 코드·오류 조건을 작성한다. 합의 전인 값은 미정으로 표시한다.
- 기능 동작·시나리오는 OpenSpec에 두고 계약에서는 해당 명세를 연결한다. 필드 표와 응답 예시는 이 문서가 원본이다.
- 내용이 커져 독립적인 독자·용도가 생길 때 기능별 문서로 나누고 이 문서에서 연결한다.
