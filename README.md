# 39-COLLABORATION-SERVER-DAISO
PLAY SOPT 39기 합동 세미나 모바일 앱 1조 - 다이소

## 로컬 MySQL 연결

Java 21과 실행 중인 Docker Desktop이 필요합니다. 프로젝트 루트에서 실행합니다.

1. 프로젝트 루트에 `.env`를 만들고 `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`를 설정합니다. 로컬 포트는 `3307`, DB와 계정 이름은 `daiso`를 사용하고 두 비밀번호는 직접 지정합니다. `.env`는 Git에서 제외됩니다.
2. `docker compose up -d --wait`로 MySQL 8.4를 실행합니다.
3. PowerShell에서 `./gradlew.bat bootRun`으로 서버를 실행합니다.
   IntelliJ에서는 `DaisoApplication`을 실행하고 작업 디렉터리를 프로젝트 루트로 설정합니다. 기본 프로파일이 `local`이므로 Active profiles는 비워 두어도 됩니다.

서버는 `localhost:8088`, MySQL은 `.env`의 `MYSQL_PORT=3307` 설정에 따라 `localhost:3307`을 사용합니다. MySQL 포트가 사용 중이면 `.env`의 `MYSQL_PORT`를 변경합니다. 로컬 프로필은 `.env`를 읽어 Compose와 같은 DB 계정을 사용합니다. 로컬은 JPA `update`로 테이블 변경을 자동 반영하며 Flyway는 실행하지 않습니다.

- 상태 확인: `docker compose ps`
- DB 접속: `docker compose exec mysql mysql -u daiso -p daiso` (계정·DB 이름을 변경했다면 해당 값 사용)
- 테스트(PowerShell): `./gradlew.bat test`
- 종료: `docker compose down` (데이터 볼륨은 유지됩니다.)

비밀번호와 DB 이름은 볼륨을 처음 초기화할 때 적용됩니다. 기존 볼륨의 계정 정보는 `.env` 변경만으로 바뀌지 않습니다. 로컬 연결은 루프백 주소로 제한되며 TLS를 사용하지 않습니다.

## DB 스키마 관리

- `local`: JPA `update`, Flyway 비활성화
- `prod`: Flyway migration 실행 후 JPA `validate`. 배포 DB 주소 설정은 기존 구성을 유지합니다.
- `migration`: 개인 개발 DB와 분리된 검증 DB에서 Flyway와 JPA `validate` 실행
- 초기 스키마: `src/main/resources/db/migration/V1__create_initial_schema.sql`
- ERD의 `product`, `store`, `store_inventory` 테이블을 반영했습니다. ID는 자동 생성하며 상품·매장 재고 조합은 중복을 허용하지 않습니다. 그 외 ERD에 없는 기본값·상태값 제약·공통 컬럼은 추가하지 않았습니다.
- 공유한 migration은 수정하지 않고 다음 버전 SQL을 추가합니다. 기준은 [ADR 0002](docs/adr/0002-manage-schema-with-jpa-and-flyway.md)를 따릅니다.

검증 테스트는 별도의 버릴 수 있는 MySQL DB를 준비한 다음 PowerShell에서 실행합니다. 아래 값은 검증 DB 접속 정보로 바꿉니다. `.env`의 개발 DB를 대상으로 사용하지 않습니다.

```powershell
$env:MIGRATION_DB_URL='jdbc:mysql://127.0.0.1:<검증 포트>/<검증 DB>?allowPublicKeyRetrieval=true&sslMode=DISABLED'
$env:MIGRATION_DB_USER='<검증 계정>'
$env:MIGRATION_DB_PASSWORD='<검증 비밀번호>'
./gradlew.bat test --tests org.sopt.daiso.FlywayMigrationTests --rerun-tasks
```

`MIGRATION_DB_URL`이 없으면 해당 검증 테스트는 건너뜁니다. 테스트는 migration 이력, 테이블 생성, 자동 ID, nullable 재고, 외래키와 중복 방지를 확인합니다. 엔티티 구현 전이므로 실제 엔티티 매핑 검증은 엔티티 추가 후 수행해야 합니다. 기존 테이블이 있는 공유·배포 DB는 자동 baseline하지 않으므로 최초 도입 절차를 별도로 확인합니다.

## GitHub Actions

- `ci.yml`: `dev`·`main` 대상 PR에서 Java 21로 `clean bootJar`를 실행합니다. 테스트는 실행하지 않는 JAR 빌드 검사입니다.
- `cd.yml`: `main` push 시 Docker 이미지를 GHCR에 게시하고 SSH로 서버에 배포합니다.
- 서버의 `~/apps/daiso/.env`에 MySQL 설정을 준비합니다. 배포 시 기존 `docker-compose.yml`로 MySQL을 실행하고 Spring은 `docker run`으로 실행합니다.
- 필요한 GitHub Secrets: `DEPLOY_HOST`, `DEPLOY_PORT`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`, `DEPLOY_KNOWN_HOSTS`, `GHCR_USERNAME`, `GHCR_PAT`.

워크플로 파일을 GitHub에 push해야 실제 Actions 실행을 확인할 수 있습니다.
