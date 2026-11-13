# Design

## Context

[제안](proposal.md)의 동기를 따른다. 제안 당시 기준은 머지된 dev와 작업 브랜치의 공통 커밋 `2eb9b7e`였다. Java 21·Spring Boot 4.1.2·Spring MVC를 사용하며 Swagger 의존성·설정은 없는 상태였다. 구현된 API 4개와 Controller 3개, 실제 record DTO·수동 검증·계산 getter를 조사했다.

HTTP 원본은 [API 계약](../../../../docs/api/api-contract.md), 개발·검증 절차는 [개발 가이드](../../../../docs/development.md)를 따른다. 기존 기능 명세는 변경하지 않는다. DB와 스키마 관리 선택도 그대로 유지하므로 새로운 ADR을 추가하지 않는다.

## Goals / Non-Goals

**Goals:** 기존 코드의 타입 추론을 활용하고 잘못 표현될 지점만 보완해, 프론트엔드가 올바른 요청과 실제 응답 구조를 확인할 수 있게 한다.

**Non-Goals:** Entity·업무 로직·직렬화·검증·migration 변경, 문서 전용 응답 래퍼·Docs 인터페이스·자체 어노테이션 체계, CSS·별도 UI·정적 명세 생성·CI 확장, 다른 변경의 보관은 포함하지 않는다.

## Decisions

### 의존성과 공통 설정

- `springdoc-openapi-starter-webmvc-ui:3.2.0`을 추가한다. 공식 호환표는 Boot 4.1.x에 springdoc 3.1~3.2를 안내하며, 3.2.0의 POM도 Boot 4.1.2를 기준으로 한다. 관리되는 swagger-core·Jackson 버전을 개별 덮어쓰거나 애플리케이션의 Jackson 3를 교체하지 않는다.
- OpenAPI 문서 형식은 기본 3.1 계열을 사용한다. 표시할 업무 API 버전 `v1`과 `/v3/api-docs`의 문서 경로를 구분한다.
- `global/config/OpenApiConfig.java` 하나에서 제목 `다이소 API`, 버전 `v1`, 상품·매장 태그와 같은 서버를 가리키는 상대 서버 URL `/`를 관리한다. 현재 코드에는 별도 context-path가 없다. 개인·배포 호스트를 하드코딩하지 않으며 문서 때문에 CORS·프록시 신뢰 설정을 확대하지 않는다.
- application 설정에서 `/api/v1/**`만 문서화한다. 로컬·팀 배포에서 `/swagger-ui.html`과 `/v3/api-docs`를 활성화하고 GET·PATCH 실행을 허용한다. 초기 자동 편집 모드는 끄고 사용자가 실행을 선택하게 한다. 인증 입력·문서 그룹 선택기는 추가하지 않는다.
- 기본 UI에 태그 펼침·API 상세 접기(`doc-expansion: list`), JSON 예시 우선, 하단 전체 Schemas 숨김을 적용한다. 개별 API의 필드 구조는 계속 조회할 수 있다. 테마·검색창·정렬 등의 추가 조정은 기본값으로 둔다.

### 문서화 위치와 필드 조사 결과

Controller에 짧은 제목·설명과 입력·응답 보완을 직접 작성하고 DTO에 필드 설명·예시를 둔다. 공통 `ApiResponse`와 Swagger 응답 어노테이션은 이름이 같으므로 문서 어노테이션에 필요한 완전 수식명을 사용한다. 기존 공통 응답 클래스의 이름을 바꾸거나 이 충돌만을 위한 추상화를 만들지 않는다. 반환형 추론을 쓰는 응답은 `@ApiResponses`에 선언하고 기본 문서 응답 매체를 JSON으로 맞춘다. springdoc 3.2.0에서 `@Operation.responses` 안의 추론 플래그가 적용되지 않거나 기본 */* 매체로 schema가 분리되는 것을 방지한다.

| 대상 | 생성 문서에서 확인·보완할 지점 |
| --- | --- |
| `ProductController`, `StoreController`의 JsonNode 본문 | OpenAPI RequestBody에 기존 `ProductLikeRequest`·`StoreFavoriteRequest`를 명시한다. 본문과 boolean 필드는 문서에서 필수다. Spring의 `required=false`와 수동 검증은 유지한다. |
| ID와 `ProductStoresController.soldOut` | Java 수신 타입이 String이어도 ID는 계약의 양의 int64 범위, soldOut은 선택 boolean·기본 true로 문서화한다. 입력을 받는 위치와 기존 검증을 바꾸지 않는다. |
| `ProductStoresRequest` | 내부 변환용 record이며 HTTP 본문이 아니다. 요청 본문이나 별도 공개 모델로 노출하지 않는다. |
| `ApiResponse<T>` | 메서드 반환형의 구체적인 T를 추론한다. 200 설명을 보완할 때 `useReturnTypeSchema`를 활용하고 raw ApiResponse로 덮어쓰지 않는다. 제네릭 래퍼의 schema 이름을 하나로 고정해 서로 다른 응답이 합쳐지지 않게 한다. |
| `ErrorResponse` | code·message만 표현한다. 성공 data나 ErrorType의 내부 HTTP 상태 필드를 노출하지 않는다. |
| `ProductListItemResponse` | 모든 공개 필드의 의미·예시를 설명한다. productId는 숫자 int64, productNumber는 앞자리 0을 유지하는 String, price·reviewCount는 표시용 String으로 확인한다. imageUrl·liked도 저장된 응답값으로 표현한다. |
| `ProductListResponse`, `ProductStoresResponse` | products·stores는 구체적인 항목 배열이며 빈 결과는 []다. 재고 응답의 product와 두 위치의 상품 ID는 현행 구조대로 표현하고 product는 빈 stores와 함께 반환됨을 설명한다. |
| `ProductLikeResponse`, `StoreFavoriteResponse` | 숫자 ID와 저장된 boolean 결과를 필수 응답 필드로 표현한다. |
| `ProductStoreResponse.getDistance()` | 실제 JSON에 포함되는 계산 필드 distance를 읽기 전용 String으로 명시한다. 원본 distanceMeters는 int64로 유지하며 계산 getter의 직렬화 이름·값을 바꾸지 않는다. |
| `ProductStoreResponse`의 상태·찜·재고 | businessStatus의 OPEN·CLOSED와 계약의 한국어 의미를 표시한다. bookmarked는 매장 찜 API의 favorite와 같은 상태다. stockQuantity는 int32, pickupAvailable은 boolean이며 서로 독립적인 값임을 설명한다. |
| Entity의 JPA 필드·관계 | Product·Store·StoreInventory를 API 모델로 노출하거나 Swagger 어노테이션을 추가하지 않는다. DB의 snake_case·연관관계·길이·제약이 DTO에 자동 전파된다고 가정하지 않는다. |

### 필수값·예시·오류 처리

- 필드의 존재 여부(required)와 null 허용은 구분한다. 공개 DTO에 계약의 필수 여부를 명시하고 OpenAPI 3.1의 실제 type·required·참조 결과를 검사한다. 공통 계약의 data:null 허용을 전역으로 새로 제한하지 않으며 현재 4개 API는 각 구체 응답과 실제 성공 예시로 표현한다.
- 문서 보완에는 `@Schema` 등 문서 어노테이션을 사용한다. 문서 표시를 위해 Bean Validation·Jackson 어노테이션을 추가하거나 누락값 처리·자동 형변환을 바꾸지 않는다. 추가 JSON 필드를 거부한다는 새 제약도 만들지 않는다.
- example은 설명용 값이다. 필수 liked·favorite에 false 기본값이 있는 것처럼 안내하지 않는다. 기본값은 실제 생략 동작이 있는 soldOut에만 명시한다. ID 예시가 배포 DB에 실제 존재한다고 보장하지 않고 목록 조회 결과의 ID를 사용하도록 안내한다.
- 성공 예시는 API마다 대표 1개를 두고 code·실제 message·data를 맞춘다. 필드 예시에는 문자열 타입과 경계 표기를 반영한다. 재고 0을 넣은 예시는 soldOut=false 조건을 명시하고, 거리 원본과 표시값·상단 상품 ID도 일치시킨다. 긴 업무 규칙과 표는 API 계약을 링크한다.
- `override-with-generic-response=false`로 ControllerAdvice 오류의 일괄 추가를 끄고 기존 계약의 API별 오류를 명시한다. 실패 schema는 공통 ErrorResponse를 재사용하며 같은 400의 여러 오류 코드는 짧은 설명으로 안내한다. 전체 ErrorType을 모든 응답 code의 공통 enum으로 노출하지 않는다.
- 목록은 500, 재고 조회는 400·404·500·503, 좋아요·매장 찜은 400·404·500을 중심으로 표현한다. 공통 405·415·필수값 저장 오류는 계약의 공통 규칙과 해당 API의 적용 조건을 함께 확인하며, 이 작업에서 실제 오류 처리를 바꾸지 않는다.

### 예상 변경 파일과 검증

- `build.gradle`, 공통 OpenAPI 설정 1개, `application.yml`이 설정 대상이다. Controller 3개, 요청 DTO 2개·응답 DTO 6개·공통 응답 DTO 2개가 문서화 대상이며 적용 버전이 이미 정확히 추론하는 설정은 중복 선언하지 않는다.
- `OpenApiDocumentationTest` 한 클래스에서 실제 문서 endpoint를 호출해 API 4개·입력·중첩 응답·오류를 확인한다. 내부 생성 schema 이름을 하드코딩하는 대신 응답의 참조를 따라가며 공개 필드와 JSON 타입을 검사한다. 기존 JSON 직렬화와 비교해 계산 필드 누락·필드명 변경·잘못된 enum·nullability를 잡는다. 범용 명세 검증 프레임워크나 UI 설정값을 반복하는 테스트는 만들지 않는다.
- 실제 브라우저에서 태그·접힘·예시·입력 UI를 확인하고 API 실행은 별도 검증 DB에서 확인한다. `/v3/api-docs`의 200만으로 문서가 정확하다고 판단하지 않는다. 기존 엄격한 입력·오류·DB 연동 테스트도 함께 통과시킨다.
- README에는 접속 방법, development.md에는 문서 변경·검증 기준을 통합한다. HTTP 필드·오류의 원본은 api-contract.md에 유지하고 새 사용 설명 문서는 만들지 않는다.

## Risks / Trade-offs

- 호환표가 실제 생성 결과까지 보장하지는 않는다 → Boot 4·Jackson 3와의 기동, 구체적인 제네릭 참조와 계산 getter를 실행 검증한다.
- 문서 어노테이션이 코드에 늘어난다 → 자동 추론과 짧은 DTO 설명을 우선하고, 실제 표시가 과하게 길어지면 공통 선언·기존 항목과 합칠 수 있는지 검토한다. 분리 구조가 필요해지면 구현 중 보고한다.
- 팀 배포에서 PATCH를 실행하면 공유 상태가 바뀐다 → 사용자가 허용한 동작이며 문서 설명에 명시하고 검증에서는 임시 데이터만 사용한다.
- 배포 서버의 실제 주소·프록시 경로는 코드에 확정되어 있지 않다 → 현재 루트 경로 설정에 맞춰 동일 출처 상대 URL을 사용한다. 실제 배포가 하위 경로를 쓴다면 배포 확인 시 경로만 확인하고 조정 필요성을 보고한다.

## Migration Plan

스키마 변경 없이 의존성과 문서 설정을 배포한다. Swagger가 비활성화되거나 의존성·어노테이션을 되돌려도 기존 API 계약과 DB는 유지한다. 구현 완료·검증 후 해당 변경만 현재 명세에 동기화하고 보관한다.

## References

- [springdoc 호환표](https://springdoc.org/faq.html), [3.2.0 POM](https://github.com/springdoc/springdoc-openapi/blob/v3.2.0/pom.xml): Boot 4.1.2·swagger-core 2.2.55 기준 확인.
- [springdoc 설정](https://springdoc.org/properties.html): OpenAPI 3.1, UI·경로·ControllerAdvice 응답 옵션.
- [Schema 어노테이션](https://github.com/swagger-api/swagger-core/blob/v2.2.55/modules/swagger-annotations/src/main/java/io/swagger/v3/oas/annotations/media/Schema.java), [ApiResponse 어노테이션](https://github.com/swagger-api/swagger-core/blob/v2.2.55/modules/swagger-annotations/src/main/java/io/swagger/v3/oas/annotations/responses/ApiResponse.java): requiredMode·읽기 전용·구체적인 반환형 추론.
- [OpenAPI 3.1.1](https://spec.openapis.org/oas/v3.1.1.html): 필드 존재·타입·null 표현을 별도로 확인할 근거.
