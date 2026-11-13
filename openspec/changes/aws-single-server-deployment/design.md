# Design

## Context

동기·확정 범위는 [제안](proposal.md)을 따른다. 조사 기준 `ca03701`의 CD는 main push 후 일반 Ubuntu runner에서 이미지를 만들고 SSH로 배포한다. ARM64 지정·메모리 제한·스왑 준비·실패 복구는 없다. 로컬 Compose는 MySQL만 실행하며 prod 애플리케이션은 Docker로 따로 실행한다. 사용자 AWS 계정 내 실제 리소스·비용·기본 VPC는 아직 확인하지 않았다.

[개발·검증 기준](../../../docs/development.md#검증-기준)과 [ADR 0002](../../../docs/adr/0002-manage-schema-with-jpa-and-flyway.md)의 Flyway·JPA validate 방식을 유지한다. 아래는 이번 단일 실습 서버의 설계이며 실제 적용·검증 결과가 아니다.

## Goals / Non-Goals

**Goals:** Actions에서 검증한 동일 버전을 ARM64 서버에 적용하고 제한된 메모리에서 재시작·재배포·장애 식별을 가능하게 한다. AWS 변경은 사용자 확인 범위를 명확히 한다.

**Non-Goals:** 무중단·다중 서버·고가용성, 임의 브랜치 배포, 도메인·HTTPS·별도 검색 엔진, 범용 배포 프레임워크. API 계약·데이터 모델은 바꾸지 않는다.

## Decisions

### 1. AWS 생성 후보와 승인 단계

사용자가 확정한 것은 서울·새 t4g.micro 한 대·Spring/MySQL 동거·스왑 1GiB·Elastic IP·main 자동 배포·SSM이며 기존 데이터 이전은 없다. 그 외 후보는 **생성 전 실제 화면과 요금을 확인받은 뒤** 적용한다.

| 대상 | 생성·설정 후보 | 확인받을 영향 |
| --- | --- | --- |
| EC2 | 서울, 온디맨드 t4g.micro, Ubuntu Server 24.04 LTS ARM64 일반판 | AMI 제공자·호환성·시간당 요금. 유료 Pro/Marketplace·Spot·예약 약정은 제외 |
| CPU | Standard 크레딧 모드 | 추가 CPU 크레딧 과금 대신 크레딧 소진 시 성능 제한 |
| 디스크 | 루트 gp3 20GiB 한 개, 기본 IOPS·처리량, AWS 관리 키 암호화 | 기본 저장 비용. 종료 시 삭제 후보이므로 인스턴스 종료 시 DB도 잃을 수 있음 |
| 주소 | Elastic IP 한 개를 서버에 연결 | 유지 비용, 서버 종료 후에도 주소를 해제하기 전에는 과금 가능 |
| 네트워크 | 기본 VPC의 인터넷 연결 가능한 퍼블릭 서브넷 재사용 | 기본 VPC가 없으면 별도 구성안을 제시하고 확인받음. NAT Gateway·유료 VPC Endpoint는 추가하지 않음 |
| 보안그룹 | 초기 API TCP 8088 공개 후보, SSH·MySQL 인바운드 없음 | 인증 없는 실습 API·Swagger를 외부에서 호출하고 공유 상태를 변경할 수 있음. 실제 공개 CIDR은 확인받음 |
| 선택 서비스 | 자동 백업·스냅샷 일정·상세 모니터링·유료 로그 수집 없음 | EC2·EBS·IPv4·전송의 기본 비용은 남고 DB 자동 복원 지점은 없음 |

생성 전 지역별 가격과 계정 크레딧을 읽기 전용으로 확인한다. 월 예산 상한·종료일은 아직 미정이며 임의로 확정하거나 무료라고 표시하지 않는다. 보안그룹·IMDSv2·기본 암호화처럼 기본 보호를 비용 절감 명목으로 해제하지 않는다.

승인 묶음은 다음과 같다. 실제 AWS 조작은 사용자 컴퓨터의 브라우저에서 진행한다.

1. **권한 연결:** OIDC 공급자·역할·정책 및 인스턴스 역할의 실제 신뢰 대상·권한을 제시하고 생성/변경 확인을 받는다.
2. **서버 생성:** 위 EC2·EBS·네트워크·주소의 실제 선택값·합계 비용·데이터 손실 조건을 제시하고 확인받는다. Elastic IP 할당·연결도 이 범위에 포함한다.
3. **서버 초기 설정:** 설치할 Docker·Compose·SSM Agent, 스왑·권한·재시작·로그 설정과 `.env` 준비 방법을 보여주고 적용 확인을 받는다.
4. **자동 배포 활성화:** 연결할 EC2·main·GitHub Environment·이미지 접근 범위와 자동 실행 항목을 확인받는다. 이후 main 배포는 승인한 스크립트만 실행한다. 리소스 증설·권한 확대·디스크/데이터 삭제는 자동화하지 않는다.

### 2. 서버 외부 빌드와 이미지 전달

GHCR과 Dockerfile의 런타임 구성을 재사용하되 검증한 JAR만 이미지에 복사하고 GitHub의 `ubuntu-24.04-arm` runner에서 `linux/arm64` 이미지를 만든다. x64 이미지를 EC2에서 빌드하거나 에뮬레이션하는 방법은 사용하지 않는다. Temurin·MySQL 태그의 ARM64 manifest를 구현 시 확인한다.

PR 및 배포 전 검사에서 Java 21과 일회용 MySQL을 사용해 기존 전체 테스트·문서 테스트·migration을 실행한다. DB 환경변수를 명시하고 필수 테스트가 건너뛰어지지 않았는지 확인한다. 테스트·빌드가 성공한 커밋만 GHCR에 SHA 태그로 게시하고 실제 배포는 digest로 고정한다. 서버에서 Gradle·이미지 빌드는 실행하지 않는다.

GHCR 패키지를 임의로 공개 전환하지 않는다. 비공개 pull을 기준으로 read:packages만 가진 인증을 준비하며 토큰은 서버의 제한된 권한 파일에 최초 준비한다. GitHub에서 비밀값을 SSM 명령에 삽입해 로그인시키지 않는다. 이미지 접근 인증은 AWS 장기 키와 구분한다. `.env`·비밀값을 이미지·빌드 인자·배포 명령 로그에 넣지 않는다.

### 3. GitHub OIDC와 SSM

SSH 키 대신 GitHub OIDC 임시 자격증명으로 SSM Run Command를 호출한다. 기존 팀 설정을 덮어쓰지 않도록 새 GitHub `practice-production` Environment를 사용해 main만 배포하도록 제한하고, AWS 신뢰 정책의 audience·저장소·Environment subject를 해당 저장소로 고정한다. 환경 subject만으로 브랜치가 제한되는 것은 아니므로 GitHub 쪽 main 제한도 함께 검증한다.

서버 역할은 SSM 관리 노드에 필요한 권한만, Actions 역할은 지정 EC2에 지정 문서로 명령을 보내고 결과를 읽는 권한만 부여한다. 인스턴스 생성·보안그룹 변경·IAM 변경·볼륨 삭제 권한을 Actions에 주지 않는다. IAM 리소스 범위 제한이 지원되지 않는 조회 권한은 사유와 실제 정책을 승인 화면에 따로 표시한다.

SSM Agent·인스턴스 역할·인터넷으로 나가는 HTTPS 연결을 확인한다. SSH 22 포트나 DB 포트를 공개하지 않는다. `.env`는 서버 내부의 권한 600 파일로 준비하고 사용자에게 비밀값 입력을 맡긴다. 비밀번호를 SSM 명령 파라미터·기록·GitHub 출력에 전달하지 않는다.

### 4. 실행 구성과 1GiB 스왑

로컬의 `docker-compose.yml`은 유지하고 배포 전용 `docker-compose.prod.yml`에 두 서비스를 묶는다. 목적이 다른 실행 설정이며 환경값·계정·기존 SQL은 재사용한다. 서버 준비는 `scripts/bootstrap-server.sh`, 배포는 `scripts/deploy.sh`에 한정하고 AWS 생성은 스크립트에 포함하지 않는다.

- `/swapfile`에 정확히 1GiB를 준비하고 권한 600·`fstab` 등록·낮은 swappiness(초기 후보 10)로 재부팅 후 유지한다. 기존 스왑이 있으면 삭제하거나 중복 생성하지 않고 크기·구성을 확인한다. 스왑은 RAM 증설이나 성능 보장이 아니다.
- 초기 검증 후보: Spring 컨테이너 RAM 384MiB·RAM+swap 768MiB, JVM heap 최대 192MiB; MySQL RAM 320MiB·RAM+swap 576MiB, buffer pool 64MiB·연결 수 20. Docker의 memory-swap은 RAM을 포함한 총량이므로 단위를 혼동하지 않는다. 호스트·Docker·SSM 공간을 남기고 실제 RSS·swap·OOM·기동 시간을 측정해 조정한다.
- DB의 performance_schema 등 메모리 소비 설정은 실제 초기화와 조회를 확인하며 필요한 범위에서 조정한다. 메모리 제한만 낮춰 기동 성공으로 간주하지 않고 동시 소량 요청·재부팅도 확인한다.
- MySQL은 Docker 내부 네트워크에서만 접근하며 데이터는 명명 볼륨에 둔다. 앱은 기존 `prod`·Flyway·JPA validate와 비관리자 DB 계정을 사용한다. 애플리케이션의 DB 연결 풀도 작은 연결 수로 제한한다.
- 컨테이너는 재부팅 후 다시 실행되도록 설정하고 DB health를 기다린 뒤 앱을 시작한다. 로그는 서비스별 10MiB × 3개 정도의 초기 후보로 회전한다. 사용 중 이미지·DB 볼륨을 지우는 일괄 prune을 넣지 않는다.

### 5. 배포 순서와 실패 처리

main의 배포를 직렬화한다. Actions는 해당 커밋의 Compose·배포 스크립트를 고정 버전으로 전달하고 SSM에는 고정된 배포 진입점만 실행시킨다. 공개 저장소의 파일을 받을 때도 main/latest URL이 아닌 해당 커밋 URL을 사용한다.

1. 서버·배포 파일·설정의 존재와 이미지 digest를 확인하고 새 이미지를 먼저 pull한다. 이전 버전 정보를 보존한다.
2. DB를 준비하고 기존 볼륨을 유지한 채 앱을 교체한다. 단일 서버이므로 짧은 중단을 허용하고 두 앱을 동시에 띄워 메모리를 소모하는 무중단 방식은 쓰지 않는다.
3. SSM 명령 ID의 최종 결과와 종료 코드를 기다리고, 타임아웃·실패를 Actions 실패로 전파한다. 명령 접수만으로 완료 처리하지 않는다.
4. 서버 내부 health에 이어 외부의 health·Swagger UI·`/v3/api-docs`·대표 API를 확인한다. 공개 주소에서의 실패를 로컬 성공으로 대체하지 않는다.
5. 실패 시 직전 digest·새 로그·DB migration 상태를 확인한다. DB 변경이 없고 호환되는 경우 이전 앱 이미지를 복구한다. migration이 적용됐거나 불명확하면 자동 이미지 복구를 중단하고 별도 판단한다. 최초 배포에는 직전 이미지가 없음을 명시한다.

## Risks / Trade-offs

- 1GiB RAM과 스왑에서도 기동 실패·느린 응답 가능 → EC2 실제 부하·OOM·재시작을 확인하고 실패하면 조정안을 보고한다. 사용자 확인 없이 인스턴스를 키우지 않는다.
- Standard 모드에서 CPU 크레딧 소진 시 느려짐 → 추가 과금과 성능의 절충을 설명하며 Unlimited를 임의로 켜지 않는다.
- 자동 백업 없음 → 컨테이너 재배포는 볼륨을 유지하지만 서버·디스크 손실까지 복구하지 못한다. 삭제·초기화는 매번 영향과 별도 확인이 필요하다.
- 최초 main은 아직 초기 커밋 상태이므로 workflow가 main에 들어가기 전에는 main 자동 배포가 시작되지 않음 → 기존 dev 통합 코드를 팀 절차대로 검토·병합하고 이를 AWS 생성과 구분한다. main 병합은 별도 요청 범위다.
- 초기 HTTP·IP 접근 → HTTPS·도메인은 추후 변경이며 모바일 앱의 HTTP 허용 정책이나 웹 프론트 CORS가 필요하면 구체적인 연동 주소·증상으로 별도 결정한다. Swagger 자체는 같은 서버 주소로 실행한다.

## Migration Plan

배포 스크립트·검증을 먼저 준비한 뒤 승인 단계에 맞춰 AWS와 서버를 준비한다. 새 DB에는 기존 V1~V3를 적용하고 기존 더미 SQL은 사용자 확인 후 한 번만 적재한다. 배포 자동 실행에 더미 삽입을 넣지 않는다. 구현·실제 외부 검증 완료 후 README와 개발 가이드의 이전 SSH 설명을 교체하고 현재 명세 동기화·보관을 수행한다.

## Open Questions

- 생성 시 실제 AMI ID·서브넷·공개 CIDR·역할/서버 이름·레지스트리 접근 상태와 예상 비용을 확인받는다. 월 예산 상한·사용 종료일·더미 적재 여부도 아직 확정하지 않는다.
- 초기 메모리·연결 풀·로그 후보값은 구현 및 실제 EC2 검증으로 확정한다. 1GiB 스왑과 t4g.micro라는 사용자 선택은 임의로 바꾸지 않는다.

## References

- [AWS t4g 사양](https://aws.amazon.com/ec2/instance-types/t4/), [CPU 크레딧](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/burstable-performance-instances-unlimited-mode.html), [IPv4 요금](https://aws.amazon.com/vpc/pricing/)
- [SSM Run Command](https://docs.aws.amazon.com/systems-manager/latest/userguide/run-command.html), [GitHub OIDC와 AWS](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws)
- [GitHub runner](https://docs.github.com/en/actions/reference/runners/github-hosted-runners), [Docker 메모리·스왑 제한](https://docs.docker.com/engine/containers/resource_constraints/)
