# Tasks

## 1. 상품 저장

- [x] 1.1 현재 V1에 매핑하는 Product·ProductRepository와 changeLiked 메서드를 추가한다. 별도 MySQL의 V1 적용 및 JPA validate로 숫자·nullable 필드 매핑을 확인한다.
- [x] 1.2 ProductService와 응답 DTO를 추가해 요청 값 저장·해제·반복 요청을 구현한다. 별도 MySQL 테스트로 커밋, 다른 상품·컬럼 보존 및 실패 시 롤백을 검증한다.

## 2. HTTP 계약

- [x] 2.1 Controller·DTO와 필요한 상품 오류를 구현하고 기존 global 응답·예외 처리를 재사용한다. HTTP 테스트로 성공 응답과 본문·ID 오류 및 int64 경계, 상품 없음, 내부 정보 비노출, 405·415 계약과 응답 헤더 보존을 확인한다.
- [x] 2.2 API 계약과 실제 구현·테스트를 대조하고 검증된 범위에 맞춰 미구현 표시 및 필요한 개발·검증 안내를 갱신한다.

## 3. 통합 검증

- [x] 3.1 bootJar testClasses, 상품·기존 매장 HTTP 테스트와 별도 MySQL 통합 검증을 수행하고 실행·실패·생략 범위를 구분한다. OpenSpec strict 검증과 문서 링크·diff 검사로 전체 정합성을 확인한다.

## Workflow follow-up

- 구현·검증 및 사용자 검토 후 현재 명세 동기화·archive 필요 여부를 마무리 기준에 따라 처리한다.
