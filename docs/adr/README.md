# ADR (Architecture Decision Record)

프로젝트의 주요 기술·설계 결정과 그 이유를 기록합니다.
팀 전체에 영향을 주거나 변경 비용이 큰 결정을 대상으로 합니다.

작성·수정 시 [ADR 작업 지침](AGENTS.md)을 따른다.

MADR 형식과 생성 템플릿은 [template.md](template.md)에서 관리한다. 생성·검색·상태 변경은 [개발 도구 사용법](../development.md#adr-조회작성)을 따른다.

## 작성 기준

- 여러 기능에 영향을 주거나 변경 비용·위험이 큰 구조·의존성·저장 방식·보안 선택의 배경·대안·결론·영향을 한 ADR에 기록한다. 단순 설정·서식 변경은 이슈·PR에서 다룬다.
- 현재 컨벤션·개발 절차는 [개발 가이드](../development.md)를 원본으로 유지하고 ADR에서는 관련 문서를 연결한다.
- adrs MCP 또는 `npm run adr -- <명령>`으로 생성·검색·상태·대체 관계를 관리한다. 파일명은 `NNNN-kebab-case-title.md`이며 기존 번호는 재사용하지 않는다. 원문이 없는 0001도 예약 번호로 둔다.
- [MADR 템플릿](template.md)의 YAML 필드와 영문 구조 제목을 유지하고 본문은 한국어로 작성한다. 날짜는 Asia/Seoul 기준이며 생성한 UTC 날짜와 다르면 YAML date를 맞춘다.
- YAML status가 상태의 원본이다. proposed는 합의 전 제안, accepted는 채택, superseded는 후속 결정으로 대체된 상태다. 본문에 별도 상태 필드를 복제하지 않는다.
- 제안(proposed)은 검토 과정에서 본문을 수정할 수 있다. 채택(accepted)된 결정의 내용·배경은 덮어쓰지 않고, 변경 시 새 ADR과 대체 관계를 연결한다. 상태·관련 링크·오탈자는 기존 문서에 반영할 수 있다.
- 새 ADR·상태 변경 시 아래 목록도 갱신하고 `npm run adr -- doctor`로 검사한다. 관련 이슈·PR이 있으면 링크한다. 알려진 경고의 원인·처리는 [검증 기준](../development.md#검증-기준)를 따른다.

## ADR 목록

| 번호 | 결정 | 상태 |
| --- | --- | --- |
| 0001 | Database Choice — 원문 확인 필요 | 미확인 |
| 0002 | [로컬은 JPA 자동 반영, 배포 환경은 Flyway로 스키마 관리](./0002-manage-schema-with-jpa-and-flyway.md) | 채택 |
| 0003 | [MADR과 adrs 및 OpenSpec으로 개발 문서를 관리한다](./0003-madr과-adrs-및-openspec으로-개발-문서를-관리한다.md) | 채택 |

0001은 원문이 저장소에 없다. 번호는 재사용하지 않으며, 원문 복구와 결정 상태는 팀 확인 후 반영한다.
