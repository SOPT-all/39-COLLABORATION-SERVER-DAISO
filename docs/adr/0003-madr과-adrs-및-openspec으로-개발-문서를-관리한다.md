---
number: 3
title: MADR과 adrs 및 OpenSpec으로 개발 문서를 관리한다
status: proposed
date: 2026-11-12
---

# MADR과 adrs 및 OpenSpec으로 개발 문서를 관리한다

## Context and Problem Statement

현재 개발 절차와 규칙, 중요한 결정의 근거, 기능 변경 명세가 서로 중복되지 않으면서 Codex 작업에서도 유지되어야 한다. 사용자는 실습 프로젝트를 참고한 MADR·adrs MCP·OpenSpec 적용을 요청했다. 기존 ADR 번호와 내용은 보존하고 팀 합의 전인 사항을 임의로 확정하지 않아야 한다.

## Considered Options

- 기존 Markdown만 수동으로 관리한다.
- MADR 형식과 adrs로 결정 기록을 관리하고 OpenSpec으로 기능 변경을 관리한다.
- 규칙·결정·기능 변경을 모두 OpenSpec이나 ADR 한 곳에서 관리한다.

## Decision Outcome

두 번째 방식을 적용한다. MADR은 결정 기록의 형식, adrs CLI와 MCP는 생성·검색·상태·대체 관계 관리, OpenSpec은 기능 요구사항·시나리오와 변경 제안 관리에 사용한다. 현재 규칙·컨벤션과 개발 절차는 개발 가이드, HTTP 계약은 API 계약을 원본으로 유지하고 관련 명세와 결정은 링크한다.

기존 ADR 0002의 번호·파일명·결정 내용과 제안 상태를 보존해 MADR 메타데이터와 구조 제목으로 변환한다. 원문이 없는 0001은 재사용하지 않는다. 도구를 적용한 사실과 팀의 결정 채택은 구분하므로 이 ADR도 팀 합의 전까지 제안 상태를 유지한다.

현재 설치·연결·검증 방법은 [하네스 가이드](../harness.md), 문서별 원본과 갱신 기준은 [개발 가이드](../development.md#문서별-원본과-갱신-기준)에서 관리한다.

### Consequences

Good, because 중요한 결정의 근거와 현재 규칙·기능 명세의 원본을 구분하고 에이전트가 관련 정보를 조회할 수 있다.

Bad, because Node.js·Python과 adrs 실행 파일 준비가 추가되며 도구 버전 변경 시 설정·명세·연결 호환성을 재검증해야 한다.
