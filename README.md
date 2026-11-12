# 39-COLLABORATION-SERVER-DAISO
PLAY SOPT 39기 합동 세미나 모바일 앱 1조 - 다이소

## 로컬 MySQL 연결

Java 21과 실행 중인 Docker Desktop이 필요합니다. 프로젝트 루트에서 실행합니다.

1. 프로젝트 루트에 `.env`를 만들고 `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`를 설정합니다. 로컬 포트는 `3307`, DB와 계정 이름은 `daiso`를 사용하고 두 비밀번호는 직접 지정합니다. `.env`는 Git에서 제외됩니다.
2. `docker compose up -d --wait`로 MySQL 8.4를 실행합니다.
3. PowerShell에서 `./gradlew.bat bootRun`으로 서버를 실행합니다.
   IntelliJ에서는 `DaisoApplication`을 실행하고 작업 디렉터리를 프로젝트 루트로 설정합니다. 기본 프로파일이 `local`이므로 Active profiles는 비워 두어도 됩니다.

서버는 `localhost:8088`, MySQL은 `.env`의 `MYSQL_PORT=3307` 설정에 따라 `localhost:3307`을 사용합니다. MySQL 포트가 사용 중이면 `.env`의 `MYSQL_PORT`를 변경합니다. 로컬 프로필은 `.env`를 읽어 Compose와 같은 DB 계정을 사용합니다. Hibernate의 자동 테이블 생성은 비활성화되어 있습니다.

- 상태 확인: `docker compose ps`
- DB 접속: `docker compose exec mysql mysql -u daiso -p daiso` (계정·DB 이름을 변경했다면 해당 값 사용)
- 테스트(PowerShell): `./gradlew.bat test`
- 종료: `docker compose down` (데이터 볼륨은 유지됩니다.)

비밀번호와 DB 이름은 볼륨을 처음 초기화할 때 적용됩니다. 기존 볼륨의 계정 정보는 `.env` 변경만으로 바뀌지 않습니다. 로컬 연결은 루프백 주소로 제한되며 TLS를 사용하지 않습니다.

## GitHub Actions

- `ci.yml`: `dev`·`main` 대상 PR에서 Java 21로 `clean bootJar`를 실행합니다. 테스트는 실행하지 않는 JAR 빌드 검사입니다.
- `cd.yml`: `main` push 시 Docker 이미지를 GHCR에 게시하고 SSH로 서버에 배포합니다.
- 서버의 `~/apps/daiso/.env`에 MySQL 설정을 준비합니다. 배포 시 기존 `docker-compose.yml`로 MySQL을 실행하고 Spring은 `docker run`으로 실행합니다.
- 필요한 GitHub Secrets: `DEPLOY_HOST`, `DEPLOY_PORT`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`, `DEPLOY_KNOWN_HOSTS`, `GHCR_USERNAME`, `GHCR_PAT`.

워크플로 파일을 GitHub에 push해야 실제 Actions 실행을 확인할 수 있습니다.
