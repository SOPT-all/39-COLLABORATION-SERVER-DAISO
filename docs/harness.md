# 하네스 설치와 설정

설치·설정·연결 검증을 관리한다. 개발 중 도구 선택과 사용은 [개발 가이드](development.md#개발-도구-사용)를 따른다. 이 도구 구성을 선택한 근거는 [ADR 0003](adr/0003-madr과-adrs-및-openspec으로-개발-문서를-관리한다.md)을 참고한다.

## 설치

macOS/Linux의 arm64 또는 x86_64, Node.js 22.17.0 이상과 Python 3를 사용한다. 프로젝트 루트에서 실행한다. Windows에서는 이 설치 스크립트가 지원하는 WSL의 Linux 환경을 사용한다.

```sh
npm ci
npm run tools:setup
```

OpenSpec은 package.json·package-lock.json으로 버전을 고정한다. scripts/setup-tools.py는 adrs 배포 파일의 SHA-256을 확인해 .tools/bin/adrs에 설치한다. 생성물은 Git에서 제외하며 애플리케이션 빌드는 기존 Gradle을 사용한다.

## MADR·adrs MCP 설정

- [adrs.toml](../adrs.toml): ADR 디렉터리, NextGen YAML 메타데이터와 [MADR 템플릿](adr/template.md)을 지정한다.
- [.codex/config.toml](../.codex/config.toml): Git 루트의 scripts/adrs로 stdio MCP 서버를 실행한다. 개인 절대 경로나 인증 정보는 필요하지 않다.
- 프로젝트 설정을 읽을 수 있는 신뢰된 저장소에서 사용한다. CLI 등록 설정은 `codex mcp get adrs --json`으로 확인하고, 도구 설치 후 새 Codex 채팅에서 adrs 도구가 보이는지 확인한다. 설정 파일 작성과 채팅에 도구가 로드된 상태는 구분한다.

## OpenSpec 설정

[openspec/config.yaml](../openspec/config.yaml)에서 스키마·산출물 언어·작성 조건을 관리하고, .agents/skills/openspec-*로 Codex에 연결한다.

CLI로 생성한 Skills는 직접 수정하지 않는다. 버전 변경 후 `npm ci`와 `npm run openspec -- update`로 갱신하고 연결 검증을 다시 수행한다.

## 연결 검증

```sh
npm run harness:check
npm run openspec -- list --json
```

harness:check는 임시 디렉터리에서 ADR 생성·상태 변경, MCP 초기화·도구 목록·기존 ADR 조회, OpenSpec 임시 기능 명세의 strict 검증과 문서의 로컬 파일·내부 절 링크를 확인한다. 실제 ADR 상태·번호와 기능 명세는 변경하지 않는다.

OpenSpec list는 이 저장소의 프로젝트 인식과 변경 목록을 확인한다. 이 검사는 실제 기능의 구현·검증 완료를 의미하지 않는다. 작업 중 명세·ADR 검증은 [개발 가이드](development.md#검증-기준)를 따른다.
