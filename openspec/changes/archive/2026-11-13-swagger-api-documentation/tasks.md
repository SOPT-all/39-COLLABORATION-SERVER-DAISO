# Tasks

각 항목은 구현·검증을 마치면 완료 표시한다. [설계](design.md)의 파일·필드별 기준과 [API 계약](../../../../docs/api/api-contract.md)을 따른다.

## 1. 문서 제공과 기본 화면

- [x] 1.1 springdoc 3.2.0 의존성을 추가하고 실제 해석된 swagger-core·Jackson 버전을 확인한다. `bootJar testClasses`와 기동으로 호환성을 확인하며 기존 Jackson 구성을 교체하지 않는다.
- [x] 1.2 공통 OpenAPI 설정 하나와 application 설정을 추가한다. OpenApiDocumentationTest에서 UI 진입 경로·정상 OpenAPI JSON·업무 API 4개·태그·같은 서버의 실행 주소를 확인하고 로컬·팀 배포 프로필에서 문서가 활성화되는지 확인한다.
- [x] 1.3 README에 실제 접속 주소와 기존 조회 결과의 ID 사용 방법을 통합한다. 문서 경로와 서버 포트가 실제 설정과 일치하는지 확인한다.

## 2. 입력·응답·오류 표현

- [x] 2.1 Controller 3개와 공개 요청 DTO 2개에 설명을 추가한다. 생성 문서에서 필수 본문·필수 boolean, ID의 int64 범위, 선택 keyword·soldOut과 기본값을 확인하고 기존 입력 거부 테스트가 유지되는지 검증한다.
- [x] 2.2 응답 DTO 6개와 공통 DTO 2개를 문서화한다. 문서 테스트에서 API별 제네릭 data 참조, 공개 필드 이름·타입·필수 여부와 실제 직렬화 JSON을 대조한다. 표시 문자열·계산 distance·영업 Enum·bookmarked·빈 배열의 예시와 구조가 맞는지 확인한다.
- [x] 2.3 API별 성공·실패 설명과 예시를 추가한다. 자동 오류 일괄 반영을 끄고 문서 테스트로 성공 data의 타입이 보존되는지, 실패에 data가 없는지, 같은 400의 코드와 재고 503이 해당 계약과 일치하는지 확인한다.
- [x] 2.4 development.md에 Swagger 작성·검증 기준을 기존 흐름과 통합한다. 계약의 정책을 새로 확정하지 않고 README·API 계약·OpenSpec과 링크·역할이 중복되지 않는지 확인한다.

## 3. 통합 검증

- [x] 3.1 실제 브라우저에서 태그·접힘·필드·예시·요청 입력을 확인하고 별도 검증 DB의 상품으로 GET·PATCH를 실행해 기존 응답과 공유 상태 반영을 확인한다. 검증 편의로 개인·공유 DB를 수정하지 않는다.
- [x] 3.2 별도 MySQL과 migration 환경변수로 전체 기존 테스트와 새 문서 테스트·빌드를 실행해 실패·건너뜀 여부를 확인한다. Swagger를 위해 Entity·업무 로직·직렬화·migration이 바뀌지 않았는지 diff를 검토한다.
- [x] 3.3 HTTP 계약·생성 문서·동작 시나리오를 최종 대조하고 OpenSpec strict 검증·로컬 문서 링크·git diff 검사를 통과시킨다. 미검증 배포 주소나 남은 제약이 있으면 완료 보고에서 구분한다.

## Workflow follow-up

- 구현 검토·검증 후 api-documentation을 현재 명세에 동기화하고 이 변경만 archive한다. 문서 링크와 자동 생성 안내 문구가 남았는지 확인한다.
- 작업 단위별 한글 커밋과 검증 결과를 보고하고 요청 범위에 따라 원격 반영·PR을 진행한다.
