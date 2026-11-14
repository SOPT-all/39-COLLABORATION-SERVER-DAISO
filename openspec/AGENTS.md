# 기능 변경 지도

새 기능·API 동작·업무 규칙 변경에는 OpenSpec을 자동 적용한다. 오타·문서 정리·기존 동작 복구는 제외한다.

- 제안을 먼저 제시하고 후속 구현 요청에서 apply한다. 완료·검증된 변경만 archive한다.
- 미구현·합의 대기 내용은 changes/에 두고, Draft API를 현재 specs/로 등록하지 않는다.

| 상황 | 진입점 |
| --- | --- |
| 전체 작업 순서·사용자 검토 시점 | [개발 흐름](../docs/development.md#작업-흐름) |
| 제안·구현·동기화·완료 절차 | [해당 OpenSpec Skill](../.agents/skills/) |
| API·규칙·ADR과의 역할 구분 | [문서별 원본](../docs/development.md#문서별-원본과-갱신-기준) |
| 산출물 언어·작성 조건 | [OpenSpec 설정](config.yaml) |
| 도구 선택·호출 | [사용법](../docs/development.md#openspec-사용) |
| Skill 설치·갱신 | [설정](../docs/harness.md#openspec-설정) |
