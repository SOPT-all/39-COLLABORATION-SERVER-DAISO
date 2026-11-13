# 개발 가이드

작업 순서, 변경별 검증과 스키마 변경 방법을 관리한다. 환경 준비·실행은 [README](../README.md), 도구 설치·설정은 [하네스 가이드](harness.md)를 참고한다.

## 작업 흐름

작업 범위에 맞는 지침만 읽고 다음 순서로 진행한다. 도구의 세부 절차는 해당 Skill을 따른다.

| 단계 | 수행·전환 기준 |
| --- | --- |
| 탐색 | [루트 지도](../AGENTS.md#문서-지도)에서 관련 코드·계약·명세를 확인한다. 중요한 기술 선택은 ADR을 조회하고, 팀 합의가 필요한 사항은 사용자에게 확인한다. |
| 분류 | 새 기능·API 동작·업무 규칙 변경은 [OpenSpec](../openspec/AGENTS.md)을 사용한다. 오타·문서 정리·기존 명세 복구는 제안을 생략한다. |
| 제안·검토 | openspec-propose로 변경안을 제시하고, 후속 구현 요청을 받은 뒤 구현한다. 관련 이슈에는 변경안 링크를 남긴다. |
| 구현 | 기능 변경은 openspec-apply-change, 단순 수정은 직접 구현한다. 계약·현재 규칙을 함께 갱신하고 중요한 새 선택만 ADR로 기록한다. 라이브러리 API는 실제 사용 버전의 공식 문서를 확인한다. |
| 검증 | 아래 기준과 기능 시나리오·계약을 확인한다. 실패·생략·미검증을 성공이나 작업 완료로 표시하지 않는다. |
| 마무리 | [작업 마무리](#작업-마무리)에 따라 문서 정리 여부를 확인하고 작업 단위로 커밋·보고한다. |

명세 동기화·archive는 해당 OpenSpec Skill을 사용한다. 커밋·PR 완료가 변경 보관이나 ADR 채택을 뜻하지는 않는다. PR은 [템플릿](../.github/PULL_REQUEST_TEMPLATE)에 변경 요약·검증·관련 링크를 작성한다. `main` 병합 전에는 팀원의 PR 리뷰를 받고 판단의 이유를 설명한다.

## 작업 마무리

- 문서 정리 힌트: OpenSpec의 작업 완료 표시·현재 명세 동기화·변경 archive, API 계약·개발 규칙 갱신, ADR의 논의 반영·채택/대체 상태·목록을 확인한다. 실제 변경과 관련된 항목만 확인하고, 명세 동기화·보관 후에는 반영된 명세에 미작성 목적·자동 생성 안내 문구가 남지 않았는지 살핀다.
- 이미 요청받은 정리는 완료한다. 추가 사용자 검토·요청이 필요한 정리가 남으면 최종 보고에서 **대상 파일/변경명, 필요한 조치와 이유**를 제안한다. 남은 항목이 없으면 제안을 생략한다.
- 정리·검증 후 작업 단위별로 나눠 `type: 한글 설명` 형식으로 커밋하고 변경 내용·검증 결과·커밋 해시를 보고한다. 검증 결과는 실행한 검사와 생략·미검증 항목을 구분하며, 빌드 성공을 전체 테스트 통과로 표현하지 않는다. PR·푸시는 요청 범위에 따라 수행하며 별도 작업 보고서는 만들지 않는다.

## 개발 도구 사용

### ADR 조회·작성

중요한 기술 선택은 관련 ADR을 조회하고, 기존 결정으로 설명되지 않는 선택만 새 ADR로 기록한다. 제안·채택·대체의 수정 범위는 [ADR 작성 기준](adr/README.md#작성-기준)을 따른다.

```sh
npm run adr -- list
npm run adr -- search Flyway
npm run adr -- new "결정 제목"
npm run adr -- status 3 accepted
npm run adr -- new --supersedes 3 "대체하는 결정 제목"
```

생성·조회·상태 변경은 adrs MCP로도 수행할 수 있다. 위 번호는 예시이며 실제 번호·팀 합의를 확인하고, 변경 후 ADR 목록을 갱신한다. 설정 파일 위치와 연결 확인은 [하네스 설정](harness.md)을 참고한다.

### OpenSpec 사용

| 작업 | Skill |
| --- | --- |
| 요구사항·기술 선택 탐색 | openspec-explore |
| 변경 제안 | openspec-propose |
| 검토 내용으로 변경안 갱신 | openspec-update-change |
| 구현 | openspec-apply-change |
| 완료된 동작을 현재 명세에 반영 | openspec-sync-specs |
| 완료된 변경 보관 | openspec-archive-change |

Codex에서 `$openspec-propose`처럼 호출하거나 Skills 목록에서 선택한다. `npm run openspec -- list --json`으로 진행 중인 변경을 확인한다.

단계별 상세 절차는 [.agents/skills](../.agents/skills/)의 해당 Skill을 따른다. 생성된 Skill 수정과 버전 갱신은 [하네스 설정](harness.md#openspec-설정)을 따른다.

### Swagger 문서

접속 방법은 [README](../README.md#api-문서), HTTP 필드·오류의 원본은 [API 계약](api/api-contract.md)을 따른다. 문서 설정은 `OpenApiConfig`·application 설정, 공개 필드 설명은 Controller·DTO에서 관리한다.

- JsonNode 본문과 String으로 수신하는 ID·soldOut은 문서에 실제 계약의 타입·필수 여부·기본값을 명시한다. 문서 표시를 위해 기존 수신 타입·검증·직렬화를 바꾸지 않는다.
- 성공 응답은 메서드의 구체적인 제네릭 반환형을 유지한다. springdoc 3.2.0에서는 반환형 추론을 쓰는 응답 설명을 `@ApiResponses`에 두고 `useReturnTypeSchema=true`를 사용한다. 기본 문서 응답 매체는 JSON으로 맞춰 예시와 schema가 서로 다른 매체로 나뉘지 않게 한다.
- DTO를 변경하면 `OpenApiDocumentationTest`로 생성 문서의 필드·타입·필수 여부를 실제 JSON과 대조한다. 계산 getter도 확인하고, Entity·내부 변환 DTO를 공개 모델로 추가하지 않는다. 이 테스트도 별도 migration 검증 DB 환경변수가 필요하다.

## 검증 기준

프로젝트 루트에서 Java 21과 Gradle Wrapper를 사용한다. 아래 명령은 macOS/zsh 기준이며 Windows PowerShell에서는 `./gradlew`를 `./gradlew.bat`로 바꾼다.

| 변경 범위 | 검증 | 확인 범위 |
| --- | --- | --- |
| Java·Gradle 설정 | `./gradlew bootJar testClasses` | DB 없이 JAR 생성과 테스트 코드 컴파일. 테스트 실행은 포함하지 않는다. |
| 애플리케이션·DB 연동 | `./gradlew test` | README에 따라 로컬 DB와 `.env`를 먼저 준비한다. `contextLoads`는 DB 연결이 필요하다. |
| 스키마·엔티티 | 아래 migration 검증과 변경 기능 검증 | 아래 스키마 변경 절차와 migration 파일 규칙을 따른다. |
| Compose·환경 파일 | `docker compose config --quiet` | `.env`를 준비한 뒤 설정 해석을 검사한다. DB·서버 기동 성공을 보장하지 않는다. |
| 개발 하네스 | `npm run harness:check`, `npm run adr -- doctor`, `npm run openspec -- validate --all` | 임시 저장소의 도구 통신·문서 링크와 실제 ADR·명세를 검사한다. 알려진 ADR 경고는 아래 참고. |
| 문서·지침 | `git diff --check`, 변경된 로컬 링크와 명령 확인 | 문서만 바뀌면 관련 없는 DB 테스트를 반복하지 않는다. |

ADR 검사에서 ADR011은 [ADR 목록의 원문 누락 항목](adr/README.md#adr-목록)을 확인한다. 기존 0002의 대안 표·한국어 영향 목록은 ADR016·ADR017 안내가 나올 수 있다. 기존 내용을 형식에 맞추려고 바꾸거나 경고를 숨기지 않고, 새로운 경고·오류는 별도로 확인한다.

테스트의 `SKIPPED`와 실행 성공을 구분한다. DB나 필요한 환경변수가 없어 실행하지 못한 검증은 그 사유를 밝힌다. 현재 CI의 `bootJar` 검사는 테스트 실행이나 DB 검증을 대신하지 않는다.

## 현재 적용된 스키마 관리

프로파일별 설정 파일을 기준으로 `local`은 JPA `update`·Flyway 비활성화, `prod`와 `migration`은 JPA `validate`·Flyway 활성화로 구성되어 있다. 초기 SQL은 `src/main/resources/db/migration/V1__create_initial_schema.sql`이며 `product`, `store`, `store_inventory`를 생성한다. V2는 상품의 `image_url`을 NOT NULL로 변경한다. V3는 매장 이미지와 재고 수량을 NOT NULL로 변경한다. 상품의 가격·리뷰 수는 INT를 유지하며 영업 상태는 VARCHAR(30)에 OPEN·CLOSED Enum으로 매핑한다.

아래 절차는 해당 설정에서 스키마를 변경하고 검증하는 방법이다. 선택 이유는 [ADR 0002](adr/0002-manage-schema-with-jpa-and-flyway.md)를 참고한다.

### 스키마 변경 절차

1. 로컬에서 엔티티를 변경하고 같은 PR에 Flyway SQL을 작성한다. JPA `update`는 migration을 생성하지 않는다.
2. 개발·공유·운영 DB와 분리된 빈 검증 DB에 전체 migration을 적용하고 JPA `validate` 기동과 변경 기능을 확인한다.
3. 후속 migration은 직전 스키마와 대표 데이터가 있는 검증 DB에서도 적용해 데이터 보존을 확인한다.
4. 배포 환경은 Flyway 적용 후 JPA `validate`로 매핑을 확인하며, 실패하면 배포 성공으로 처리하지 않는다.

배포 스키마의 원본은 migration SQL이다. JPA `validate`는 테이블을 수정하지 않으며 모든 제약·데이터 변환을 보장하지 않으므로 SQL과 결과도 확인한다.

V2 적용 전에는 기존 상품의 이미지 누락(null·빈 문자열)을 확인하고 실제 또는 제공받은 대체 이미지 URL을 먼저 채운다. 대체 이미지의 실제 URL은 데이터 준비 전에 제공받으며 migration에서 임의 URL을 넣거나 상품을 삭제하지 않는다. NOT NULL은 URL의 형식·접근 가능 여부를 검사하지 않으므로 유효한 URL의 준비는 데이터 적재 단계에서 확인한다. 개인 로컬의 JPA `update`만으로 기존 데이터 전환이 완료됐다고 판단하지 않는다.

V3 적용 전에는 기존 매장 이미지와 재고 수량의 NULL을 실제 값으로 준비한다. MySQL strict SQL mode에서 NULL이 남아 있으면 해당 ALTER는 실패하며 빈 문자열·0으로 자동 보정하지 않는다. MySQL DDL은 전체 롤백되지 않으므로 앞선 ALTER가 적용된 상태일 수 있다. 적용 전 백업하고 실패 시 실제 스키마·데이터와 Flyway 실패 이력을 확인한 뒤 보정 및 복구 절차를 진행한다.

### Migration 파일 규칙

- 위치: `src/main/resources/db/migration/`
- 파일명: `V{번호}__{설명}.sql` (예: `V1__create_initial_schema.sql`)
- 초기 스키마는 첫 migration에 작성하고 이후 변경은 다음 번호의 파일로 추가한다.
- PR에 올라갔거나 팀에 공유되었거나 공유·배포 DB에 적용된 migration은 수정하지 않는다. 수정이 필요하면 새 버전을 추가한다.
- 개인 로컬 DB의 자동 반영 결과를 배포 DB에 직접 복사하지 않는다. 공유·배포 DB의 DDL 변경은 migration으로 남긴다.
- 같은 DB에서 JPA `update`와 Flyway가 동시에 스키마를 변경하지 않도록 프로파일별 설정을 분리한다.

Flyway를 아직 적용하지 않은 기존 공유·배포 DB가 있다면, 최초 도입 전에 기존 스키마와 초기 migration의 관계를 확인하고 baseline 또는 별도 전환 절차를 정한다. 기존 테이블에 초기 생성 SQL을 그대로 실행하지 않는다.

### 개발용 더미 데이터

[더미 데이터 SQL](../scripts/seed-dummy-data.sql)은 제공된 시트의 상품 15개·매장 8개·상품별 매장 재고 120개를 삽입한다. 시트 ID·품번·영업 상태·좋아요·찜·재고·픽업 값을 유지하며, 재고 탭의 거리는 매장별로 동일하므로 `store.distance`에 미터 단위로 저장한다. 이미지 URL은 시트 원문을 사용한다.

스키마가 준비된 **데이터가 없는 개발 DB에서 한 번만** 실행한다. Flyway migration에 포함하거나 서버 시작 시 자동 실행하지 않는다. 기존 행을 삭제·갱신하지 않으며 중복 키 등 오류가 발생하면 중단해야 한다. 실패한 SQL 뒤에 수동으로 COMMIT하거나 mysql의 `--force` 옵션으로 계속 실행하지 않는다.

프로젝트 루트에서 파일을 복사하고 컨테이너 셸에 접속한다. macOS·Windows PowerShell에서 같은 명령을 사용한다.

```sh
docker compose cp scripts/seed-dummy-data.sql mysql:/tmp/daiso-seed.sql
docker compose exec mysql sh
```

컨테이너 셸에서 다음을 실행한다. Compose의 DB 계정·DB 이름·비밀번호를 사용하며, batch 모드에서 오류가 나면 연결 종료와 함께 미커밋 데이터가 롤백된다.

```sh
MYSQL_PWD="$MYSQL_PASSWORD" mysql --user="$MYSQL_USER" --database="$MYSQL_DATABASE" --default-character-set=utf8mb4 --batch < /tmp/daiso-seed.sql
exit
```

삽입 후 `GET /api/v1/products`, `GET /api/v1/products/1/stores?soldOut=false`로 확인한다. 프론트는 시트의 상품 ID 1~15와 매장 ID 1~8을 사용할 수 있다. 품절 데이터는 전체 23개지만 상품 1은 모든 매장에 재고가 있으므로, 품절 제외 비교는 예를 들어 상품 3으로 확인한다.

### Migration 검증

검증 테스트는 별도의 버릴 수 있는 MySQL DB를 준비한 다음 실행합니다. 아래 값은 검증 DB 접속 정보로 바꿉니다. `.env`의 개발 DB를 대상으로 사용하지 않습니다.

macOS/zsh에서는 현재 명령에만 검증 DB 설정을 전달합니다.

```sh
MIGRATION_DB_URL='jdbc:mysql://127.0.0.1:<검증 포트>/<검증 DB>?allowPublicKeyRetrieval=true&sslMode=DISABLED' \
MIGRATION_DB_USER='<검증 계정>' \
MIGRATION_DB_PASSWORD='<검증 비밀번호>' \
./gradlew test --tests org.sopt.daiso.FlywayMigrationTests --tests org.sopt.daiso.store.StoreFavoriteIntegrationTest --tests org.sopt.daiso.product.ProductLikeIntegrationTest --tests org.sopt.daiso.product.ProductListIntegrationTest --tests org.sopt.daiso.product.ProductStoresIntegrationTest --tests org.sopt.daiso.StoreInventoryMigrationTest --rerun-tasks
```

Windows PowerShell:

```powershell
$env:MIGRATION_DB_URL='jdbc:mysql://127.0.0.1:<검증 포트>/<검증 DB>?allowPublicKeyRetrieval=true&sslMode=DISABLED'
$env:MIGRATION_DB_USER='<검증 계정>'
$env:MIGRATION_DB_PASSWORD='<검증 비밀번호>'
./gradlew.bat test --tests org.sopt.daiso.FlywayMigrationTests --tests org.sopt.daiso.store.StoreFavoriteIntegrationTest --tests org.sopt.daiso.product.ProductLikeIntegrationTest --tests org.sopt.daiso.product.ProductListIntegrationTest --tests org.sopt.daiso.product.ProductStoresIntegrationTest --tests org.sopt.daiso.StoreInventoryMigrationTest --rerun-tasks
```

`MIGRATION_DB_URL`이 없으면 해당 검증 테스트는 건너뜁니다. 테스트는 migration 이력, 테이블 생성, 자동 ID, 필수 재고, 외래키와 중복 방지를 확인합니다. Store·Product 매핑과 상태 저장 API는 각각 `StoreFavoriteIntegrationTest`·`ProductLikeIntegrationTest`에서 같은 migration 프로파일로 검증합니다. 별도 검증 DB 환경변수가 있어야 실행되며, HTTP 요청 후 커밋 결과·반복 요청·롤백·다른 컬럼 보존을 확인합니다.

`FlywayMigrationTests`는 V1·V2·V3 적용 이력과 상품·매장 이미지 및 재고의 null 거부도 확인합니다. `StoreInventoryMigrationTest`는 UUID로 구분한 일회용 DB를 생성·삭제하여 V2 대표 데이터의 V3 업그레이드와 NULL 거부를 확인하므로 DB 생성·삭제 권한이 필요합니다. `ProductStoresIntegrationTest`는 품절 필터·상품별 분리·빈 결과·공유 찜 반영, 상품 목록과 동일한 상단 상품 정보 및 매장 수 증가에도 SELECT가 2회인지 확인합니다. `ProductListIntegrationTest`는 빈 검증 DB에 자체 상품 데이터를 준비해 전체·검색·정렬·특수문자·표시 경계값·저장된 이미지 URL 반환·좋아요 변경 후 재조회를 확인합니다. 위 전체 migration 검증과 별도로, 기존 V1 데이터의 이미지를 준비한 뒤 V2를 적용해 데이터 보존도 확인해야 합니다.

## 코드와 컨벤션

아래는 노션과 현재 코드에 맞춘 팀 개발 기준이다. 규칙의 존재와 코드 구현 여부를 구분하며, 미구현 클래스·구조를 현재 구현으로 간주하지 않는다.

### 브랜치·커밋

[팀 브랜치·커밋 컨벤션](https://app.notion.com/p/6a214af65e0682f2bfa7817f547fef83)을 따른다.

- 브랜치: `타입/이슈번호-작업내용` (예: `chore/4-project-harness`).
- 커밋: Conventional Commits의 `type: 한글 설명` (예: `docs: 공통 응답 규칙 반영`).
- 유형: 기능 `feat`, 버그 `fix`, 동작 보존 개선 `refactor`, 테스트 `test`, 문서 `docs`, 유지보수 `chore`, 코드 서식 `style`.

### 네이밍

[팀 네이밍 컨벤션](https://app.notion.com/p/69c14af65e06836e8ae201fbbc6e415c)의 기준을 적용한다.

| 대상 | 기준 |
| --- | --- |
| 클래스·인터페이스 | PascalCase. 클래스는 명사, 인터페이스는 명사 또는 형용사 |
| 메서드 | camelCase, 동사 중심 |
| 변수 | camelCase |
| 상수 | UPPER_SNAKE_CASE |
| 패키지 | 소문자 |
| 테스트 클래스 | Test 접미사 |

약어 표기는 프로젝트에서 통일하고 한글 발음의 로마자 표기는 피한다.

### 계층·공통 예외 처리

[팀 아키텍처](https://app.notion.com/p/32f14af65e0683aa862d01fe85cfec19)와 [공통 응답·예외 처리](https://app.notion.com/p/5c214af65e0682afbfc101d6a2c4a04d)의 충돌 없는 책임 규칙을 적용한다.

- Controller는 요청 검증·HTTP 응답을, Service는 비즈니스 흐름·트랜잭션을, Repository는 영속성을 담당한다. 의존 흐름은 Controller → Service → Repository로 유지한다.
- 조회 Service는 `@Transactional(readOnly = true)`, 변경 Service는 `@Transactional`을 사용한다.
- Entity는 의미 있는 도메인 메서드로 상태를 변경한다. Request·Response DTO를 Entity와 분리하고 Entity를 직접 응답하지 않는다.
- 공통 응답·예외 처리는 `global`에서 관리한다. 사용하지 않는 클래스·계층이나 불필요한 인터페이스는 미리 만들지 않는다.
- 오류 코드는 `global.exception.ErrorType` Enum에서 HTTP 상태·메시지와 함께 관리하며 Enum 이름을 응답 코드로 사용한다. `CustomException`은 RuntimeException을 상속하고 ErrorType을 보관하며, 공통·도메인 오류 모두 이 예외로 발생시킨다. 기능 추가 시 필요한 오류 항목만 추가하고 별도 인터페이스·예외 상속 계층을 선제적으로 만들지 않는다.
- Service는 비즈니스 예외를 발생시키고, `@RestControllerAdvice`·`@ExceptionHandler`의 전역 처리기가 응답으로 변환한다. Controller마다 같은 try-catch를 반복하지 않는다.
- 비즈니스 예외는 ErrorType의 HTTP 상태로, DTO 검증 실패는 400으로, 예상하지 못한 예외는 500으로 처리한다. 서버 오류는 로그에 남기되 내부 정보는 응답에 노출하지 않는다. 응답 필드·본문 형식은 [API 계약](api/api-contract.md#23-공통-응답)을 따른다.

### 구현 범위와 확인 사항

- Elasticsearch는 사용하지 않는다. 별도 검색 엔진 의존성·색인·연동 환경을 추가하지 않는다.
- 상품 좋아요·매장 찜의 저장 정책은 [API 계약](api/api-contract.md#24-상품-좋아요매장-찜)을 따른다. 노션 문서에 남아 있던 이전 설명을 별도 정책으로 적용하지 않는다.
- 스키마 관리의 채택된 결정과 근거는 [ADR 0002](adr/0002-manage-schema-with-jpa-and-flyway.md)를 따른다.

## 문서별 원본과 갱신 기준

| 내용 | 원본 |
| --- | --- |
| 환경 준비·실행 | [README](../README.md) |
| 개발 절차·현재 규칙·검증·합의된 컨벤션 | 이 문서 |
| API 경로·입출력·오류 계약 | [API 계약](api/api-contract.md) |
| 구현·검증된 기능 요구사항·시나리오 | OpenSpec specs/ — 완료된 변경 동기화 시 갱신 |
| 진행 중인 제안·설계·작업 | OpenSpec changes/ — [OpenSpec 지침](../openspec/AGENTS.md) 참조 |
| 중요한 선택의 배경·대안·영향 | [ADR](adr/README.md) |
| 도구 설치·설정·연결 확인 | [하네스 가이드](harness.md) |

OpenSpec은 기능 동작을 정의하고 API 필드·응답 예시는 계약으로 연결한다. 변경별 design.md는 해당 변경의 설계이며, 장기적으로 중요한 선택은 ADR로 연결한다. 이슈·PR에는 상세 명세·작업 목록 대신 OpenSpec 변경 링크를 둔다.

## 문서와 지침 유지 관리

- 통합·정리 시 결정 이력인 ADR은 현재 규칙과 구분해 보존한다. 문서 추가와 중복 정리의 공통 기준은 [루트 지침](../AGENTS.md#공통-규칙)을 따른다.
- 파일을 분리·이동하면 기존 진입점과 링크도 갱신한다. README는 독립적인 탐색·목록 역할이 있을 때만 추가한다.
- AGENTS.md에는 해당 범위의 필수 제약과 상황별 문서 지도만 남긴다. 상세 절차·명령은 기존 가이드나 Skill에 두고, 상위 지침·일반 상식·일회성 진행 상태를 반복하지 않는다.
- 지침은 적용 조건과 행동을 구체적으로 적고 링크에는 읽을 상황을 표시한다. 불필요한 전체 문서 읽기를 강제하지 않는다.
