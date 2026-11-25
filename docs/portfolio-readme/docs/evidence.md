# 검증 근거

## 버전과 팀 기여

- [#13 상품 목록·검색](https://github.com/SOPT-all/39-COLLABORATION-SERVER-DAISO/pull/13): 2026-11-13 20:04:57 KST에 dev 병합. 검색, 숫자 포맷, 좋아요의 커밋 이후 읽기를 다룹니다. 당시 PR의 97개 테스트는 최종 커밋 이전 결과라고 명시돼 있습니다.
- [#11 상품 좋아요](https://github.com/SOPT-all/39-COLLABORATION-SERVER-DAISO/pull/11): 명시적 Boolean 값을 보냅니다. 공유 상태이며 별도의 요청 순서·버전 제어는 없습니다.
- [#10 매장별 재고](https://github.com/SOPT-all/39-COLLABORATION-SERVER-DAISO/pull/10): 팀원의 작업. #13보다 먼저 열렸으나 2026-11-13 21:12:38 KST에 뒤이어 병합되었습니다. [V2](https://github.com/SOPT-all/39-COLLABORATION-SERVER-DAISO/blob/2eb9b7eca88e82f6647284350244ffe23d4cfe28/src/main/resources/db/migration/V2__require_product_image_url.sql)는 상품 이미지의 필수 제약, [V3](https://github.com/SOPT-all/39-COLLABORATION-SERVER-DAISO/blob/2eb9b7eca88e82f6647284350244ffe23d4cfe28/src/main/resources/db/migration/V3__require_store_image_and_stock.sql)는 매장 이미지와 재고 수량의 필수 제약입니다. #13 작성 당시 #10에 별도 V2가 있었다는 점은 #13 PR 본문이 남긴 기록입니다. 실제 운영 장애를 뜻하지 않습니다. #10의 150개 테스트는 팀 PR의 보고이고 이 문서 작업에서 전부 재실행하지 않았습니다.
- [#19 실습 서버 구성](https://github.com/SOPT-all/39-COLLABORATION-SERVER-DAISO/pull/19): dev 병합은 운영 배포와 다릅니다. 이 검토에서는 실제 AWS 배포나 운영 데이터 초기화를 하지 않았습니다.
- 확인 시점 GitHub 소스: [dev d2bf3ca](https://github.com/SOPT-all/39-COLLABORATION-SERVER-DAISO/commit/d2bf3cae07a44a86ce73d2c5a250b58b60a65e99), [main f9d9bfa](https://github.com/SOPT-all/39-COLLABORATION-SERVER-DAISO/commit/f9d9bfa87f7bb7521efe0a1913127221b8fa6560). #22의 문자열 형식을 포함하되, 현재 운영 런타임이 이 SHA라는 뜻은 아닙니다.

## 실제 로컬 HTTP 검증

소스: [#19 병합 결과 00f7b35](https://github.com/SOPT-all/39-COLLABORATION-SERVER-DAISO/commit/00f7b35e6735231d90da5ceabe3c44d7c9e0c3ed). 오프라인 bootJar 빌드, Java 21, 새 MySQL 9.2 로컬 데이터 디렉터리, Spring migration 프로파일. Flyway 기동 뒤 체크인된 데모 데이터만 적재했습니다. 기록한 응답은 #22 이전 가격 형식입니다.

| GET 요청 | 실제 결과 |
| --- | --- |
| /api/v1/products?keyword=1068133 | 상품 1건, 와플 핫팩 2개입, 내부 ID 9 |
| /api/v1/products?keyword=1068 | 일치 0건 |
| /api/v1/products/9/stores | 품절 제외, 매장 1곳 |
| /api/v1/products/9/stores?soldOut=false | 품절 포함, 매장 8곳 |
| /v3/api-docs | OpenAPI 경로 4개 반환 |

## 영상 출처

[Android 팀 저장소](https://github.com/SOPT-all/39-COLLABORATION-ANDROID-DAISO) README에 공개된 [팀 원본 시연 영상](https://github.com/user-attachments/assets/65d397b9-c9f7-4214-b87c-db76447b7522)을 연결합니다. 위 로컬 API 표와 팀 영상은 각자 별도 기록입니다.
