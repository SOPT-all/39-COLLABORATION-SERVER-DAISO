# 39-COLLABORATION-SERVER-DAISO
PLAY SOPT 39기 합동 세미나 모바일 앱 1조 - 다이소

## 로컬 MySQL 연결

Java 21과 실행 중인 Docker Desktop이 필요합니다. 프로젝트 루트에서 실행합니다.

1. 팀에서 개인적으로 전달받은 `.env`를 프로젝트 루트에 둡니다. 환경 예제 파일은 저장소에서 관리하지 않습니다. 로컬 포트는 `3307`, DB와 계정 이름은 `daiso`가 기본값입니다. `.env`는 Git에서 제외됩니다. Compose와 Spring이 함께 읽으므로 `export`나 따옴표 없이 `KEY=value` 형식으로 작성합니다.
2. `docker compose up -d --wait`로 MySQL 8.4를 실행합니다.
3. macOS 터미널에서 `./gradlew bootRun`, Windows PowerShell에서 `./gradlew.bat bootRun`으로 서버를 실행합니다.
   IntelliJ에서는 `DaisoApplication`을 실행하고 작업 디렉터리를 프로젝트 루트로 설정합니다. 기본 프로파일이 `local`이므로 Active profiles는 비워 두어도 됩니다.

서버는 `localhost:8088`, MySQL은 `.env`의 `MYSQL_PORT=3307` 설정에 따라 `localhost:3307`을 사용합니다. MySQL 포트가 사용 중이면 `.env`의 `MYSQL_PORT`를 변경합니다. 로컬 프로필은 `.env`를 읽어 Compose와 같은 DB 계정을 사용합니다. 프로파일별 설정과 변경 절차는 [개발 가이드](docs/development.md#현재-적용된-스키마-관리)를 참고합니다.

- 상태 확인: `docker compose ps`
- DB 접속: `docker compose exec mysql mysql -u daiso -p daiso` (계정·DB 이름을 변경했다면 해당 값 사용)
- 종료: `docker compose down` (데이터 볼륨은 유지됩니다.)

비밀번호와 DB 이름은 볼륨을 처음 초기화할 때 적용됩니다. 기존 볼륨의 계정 정보는 `.env` 변경만으로 바뀌지 않습니다. 로컬 연결은 루프백 주소로 제한되며 TLS를 사용하지 않습니다.

## macOS 실행 준비

Java 21과 Docker Desktop을 사용합니다. Gradle과 MySQL을 별도로 설치할 필요는 없습니다.

```sh
java -version
# Java 21이 선택되지 않았다면 현재 터미널에서 설정합니다.
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
open -a Docker
```

Docker Desktop이 준비된 후 `docker info`가 성공하는지 확인합니다. 위 [로컬 MySQL 연결](#로컬-mysql-연결) 순서에 따라 `.env`를 준비한 후 `docker compose config --quiet`로 설정을 확인하고 DB와 서버를 실행합니다.

IntelliJ의 Project SDK와 Gradle JVM도 Java 21로 맞춥니다. Wrapper의 실행 권한은 Git에서 관리하며, 권한이 유지되지 않는 파일 시스템에서는 `bash ./gradlew bootRun`으로 실행할 수 있습니다.

## API 문서

서버를 실행한 뒤 [Swagger UI](http://localhost:8088/swagger-ui.html)에서 구현된 API를 확인하고 호출할 수 있습니다. OpenAPI JSON은 [문서 endpoint](http://localhost:8088/v3/api-docs)에서 제공합니다. 팀 배포 서버에서도 같은 경로를 사용합니다.

요청에 필요한 ID는 조회 결과의 값을 사용합니다. 좋아요·매장 찜을 실행하면 실제 공유 상태가 변경됩니다. 필드·오류의 원본은 [API 계약](docs/api/api-contract.md), 문서 수정·검증 기준은 [개발 가이드](docs/development.md#swagger-문서)를 참고합니다.

## GitHub Actions

- `ci.yml`: `dev`·`main` 대상 PR에서 Java 21로 `clean bootJar`를 실행합니다. 테스트는 실행하지 않는 JAR 빌드 검사입니다.
- `cd.yml`: `main` push 시 Docker 이미지를 GHCR에 게시하고 SSH로 서버에 배포합니다.
- 서버의 `~/apps/daiso/.env`에 MySQL 설정을 준비합니다. 배포 시 기존 `docker-compose.yml`로 MySQL을 실행하고 Spring은 `docker run`으로 실행합니다.
- 필요한 GitHub Secrets: `DEPLOY_HOST`, `DEPLOY_PORT`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`, `DEPLOY_KNOWN_HOSTS`, `GHCR_USERNAME`, `GHCR_PAT`.

CI의 JAR 빌드 성공은 DB 연결이나 Flyway migration 검증 성공을 의미하지 않습니다.

## 개발 문서

- [작업 지침](AGENTS.md): 공통 규칙과 작업별 문서 진입점
- [개발 가이드](docs/development.md): 개발 절차, 검증 방법, 합의가 필요한 컨벤션
- [API 계약](docs/api/api-contract.md): API 주소와 계약, 미정 사항
- [ADR 목록](docs/adr/README.md): 설계 결정과 템플릿
- [하네스 설치와 설정](docs/harness.md): MADR·adrs MCP·OpenSpec 설치·설정·연결 검증
