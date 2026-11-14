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

## GitHub Actions와 배포 준비

이 브랜치에는 SSM 배포 코드가 있으며 **EC2·SSM·서버 초기 설정과 AWS OIDC·GitHub Environment 연결은 준비했습니다. SSM 세션에서 첫 배포를 완료하고 외부 health·Swagger·상품 목록 응답을 확인했습니다. main 자동 배포는 아직 검증하지 않았습니다.** 서버 생성·권한·초기 설정·자동 배포 활성화는 각각 사용자 확인 후 진행합니다.

- `ci.yml`: `dev`·`main` 대상 PR에서 ARM64 runner·Java 21·일회용 MySQL로 전체 테스트와 JAR·이미지 빌드를 검사합니다. 실패하거나 건너뛴 테스트가 있으면 통과하지 않습니다.
- `cd.yml`: `main` push 시 같은 검사를 재사용하고 검증된 JAR의 이미지를 GHCR에 게시합니다. OIDC·SSM으로 지정 EC2에 digest가 고정된 이미지를 배포한 뒤 외부 health·Swagger·API를 확인합니다.
- `reset-demo-data.yml`: main 수동 실행과 확인 체크로 기존 데모 데이터셋을 복원합니다. 사용 조건·중단 시간·검증은 [데모 초기화](docs/development.md#데모-데이터-초기화)를 참고합니다.
- `docker-compose.prod.yml`: Spring·MySQL을 함께 실행합니다. MySQL 포트는 외부에 공개하지 않고 DB 볼륨을 유지합니다. 로컬 Compose와 별개입니다.
- 새 GitHub Environment `practice-production`에 **main만 배포 허용**을 설정하고 Variables `AWS_ROLE_ARN`, `INSTANCE_ID`, `BASE_URL`을 등록합니다. 현재 배포 환경에는 세 변수를 등록했고 main 브랜치만 허용했습니다. `BASE_URL`은 후행 `/` 없는 주소(예: `http://고정IP:8088`)입니다. AWS OIDC 신뢰도 이 저장소·Environment로 제한합니다.
- 서버에는 `/opt/daiso/.env`를 root 소유·권한 600으로 준비합니다. 필수값은 `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, 기본값은 `MYSQL_DATABASE=daiso`, `MYSQL_USER=daiso`입니다. 앱 포트는 8088이며 DB·비밀번호는 이미지에 포함하지 않습니다.
- 이미지 주소는 개인 계정의 `ghcr.io/sungkong00/39-collaboration-server-daiso`입니다. 팀 코드 저장소와 AWS OIDC 대상은 그대로 유지합니다. 조직의 패키지 공개 제한 때문에 개인 GHCR을 선택했으며, 서버는 공개 이미지를 인증 없이 받습니다. 현재 첫 ARM64 이미지 게시·패키지 공개 전환·로컬에서 인증 없는 pull을 확인했습니다. 새 패키지를 만들 때도 공개 전환과 익명 pull을 확인합니다. 저장소 공개 여부와 패키지 공개 여부는 별개입니다.
- 게시 인증은 `SungKong00`의 classic PAT(`write:packages`, `repo` 권한 제외)를 저장소 Actions Secret `GHCR_PUBLISH_TOKEN`에 등록합니다. 빌드 작업에서 사용하므로 Environment Secret이 아닌 **Repository Secret**입니다. `cd.yml`이 재사용 CI에 명시적으로 전달하며 PR 검사는 토큰 없이 수행합니다. 토큰 만료·폐기 시 같은 Secret을 교체합니다. AWS 인증은 별도의 OIDC이며 서버에는 이 토큰을 저장하지 않습니다. 기존 팀 Secrets는 변경하지 않습니다.

서버 초기 준비·배포·실패 복구·종료 시 주의사항은 [배포 작업](docs/development.md#배포-작업)을 참고합니다. Dockerfile은 `bootJar` 결과물을 복사하므로 로컬 이미지 검사는 `./gradlew bootJar` 후 `docker build --platform linux/arm64 -t daiso:local .` 순서로 실행합니다. 서버에서는 빌드하지 않습니다.

## 개발 문서

- [작업 지침](AGENTS.md): 공통 규칙과 작업별 문서 진입점
- [개발 가이드](docs/development.md): 개발 절차, 검증 방법, 합의가 필요한 컨벤션
- [API 계약](docs/api/api-contract.md): API 주소와 계약, 미정 사항
- [ADR 목록](docs/adr/README.md): 설계 결정과 템플릿
- [하네스 설치와 설정](docs/harness.md): MADR·adrs MCP·OpenSpec 설치·설정·연결 검증
