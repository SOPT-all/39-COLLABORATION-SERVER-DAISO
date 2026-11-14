# Proposal

## Why

실습 API를 본인 AWS 계정에서 재현 가능하게 배포하고 원격 Swagger·API 접근을 검증해야 한다. t4g.micro의 작은 메모리를 고려해 빌드는 GitHub Actions에서 끝내고 서버는 실행만 담당하도록 한다.

## What Changes

- 서울의 새 t4g.micro 한 대에서 Spring과 MySQL을 실행하고 Elastic IP로 주소를 고정한다. 기존 데이터 이전은 없고 도메인·HTTPS는 후속 범위다.
- main push를 계기로 ARM64 이미지의 검증·게시·배포를 자동 실행한다. 임의 브랜치 수동 배포나 브랜치별 서버는 만들지 않는다.
- 기존 SSH 배포를 GitHub OIDC와 AWS Systems Manager(SSM) Run Command 방식으로 교체한다. AWS 장기 액세스 키나 배포용 SSH 키를 사용하지 않는다.
- 서버에 영구 스왑 1GiB, 컨테이너·JVM·MySQL 메모리 조정, 로그 용량 제한과 재부팅 후 실행 복구를 준비한다.
- AWS 생성·변경 전 대상·옵션·예상 비용·공개 범위·데이터 영향에 대해 사용자 확인을 받는다. 자동 백업 등 선택 유료 기능은 제외하되 기본 자원 비용은 별도로 확인한다.
- 배포 실패를 성공으로 표시하지 않고 직전 이미지와 DB 상태를 구분해 복구할 수 있게 한다.

- main에서 확인 체크 후 수동 실행하는 데모 데이터 초기화를 제공한다. 기존 SQL을 재사용하고 배포와 분리하며 실제 초기화·main 병합은 별도 실행 범위다.

## Capabilities

### New Capabilities

- `service-deployment`: 검증된 main 버전의 외부 빌드·단일 서버 배포, 데이터 유지, 실패 식별과 승인받은 인프라 변경.

### Modified Capabilities

없음. 기존 API·Swagger 계약은 유지하고 원격 환경에서 같은 동작을 검증한다.

## Impact

- [이슈 #18](https://github.com/SOPT-all/39-COLLABORATION-SERVER-DAISO/issues/18), 브랜치 `chore/18-aws-deployment`, 조사 기준 `dev`의 `ca03701`.
- `.github/workflows/ci.yml`·`cd.yml`, 배포용 Compose·서버 준비 및 배포 스크립트가 구현 대상이다. 기존 Dockerfile은 ARM64 지원을 확인해 재사용한다.
- README는 실제 배포 진입점·설정값, [개발 가이드](../../../docs/development.md)는 배포·검증·복구 절차를 구현 후 갱신한다. 현재 SSH 설명을 미리 완료 상태로 바꾸지 않는다.
- AWS EC2·EBS·Elastic IP·보안그룹·IAM/OIDC, GitHub Environment와 GHCR 설정이 관련된다. 실제 준비·검증 상태는 작업 목록에서 관리한다.
- 상세 후보값·승인 단계는 [설계](design.md), 완료 판정은 [작업 목록](tasks.md)을 따른다.
