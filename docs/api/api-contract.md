# API Contract

> 문서 유형: API Contract
> 상태: Draft
> 정본 위치: `docs/api/api-contract.md`
> 기준: [팀 API 명세서](https://app.notion.com/p/API-65614af65e0683fc8032815fa74f5650?source=copy_link) — 본문 확인 대기
> 버전: v0.1
> 최종 수정: 2026-11-11

## 1. 목적

프론트엔드와 백엔드가 같은 API 주소를 기준으로 개발하기 위한 API 주소 목록이다.
사용자가 제공하는 팀 API 명세를 기준으로 작성하며, 회원 인증 API은 구현대상에 없다.

## 2. 공통 규칙

### 2.1 Base URL

| 환경 | Base URL |
| --- | --- |
| local | `http://localhost:8088` (공통 API 경로 접두사는 미정) |
| dev/staging | 미정 |
| production | 미정 |

### 2.2 인증

회원 인증을 사용하지 않는다. 
회원가입·로그인·로그아웃·토큰 재발급 등 회원 인증 API와 JWT 인증 헤더는 계약에 포함하지 않는다.

## 3. API 주소 목록

- 아래 Path는 `/api/v1` 이후 경로만 적었다.
- API 주소 수: `4`

| Method | Path | 설명 | 담당자 |
| --- | --- | --- | --- |
| `PATCH` | `/stores/{storeId}/favorite` | 관심매장 찜 | 김가영 |
| `GET` | `/products/{productId}/stores` | 상품 재고 조회 | 김가영 |
| `GET` | `/products` | 상품 목록 조회 | 노성빈 |
| `PATCH` | `/products/{productId}/like` | 상품 좋아요 | 노성빈 |


