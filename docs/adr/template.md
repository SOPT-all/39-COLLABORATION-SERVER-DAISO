---
number: {{ number }}
title: {{ title | tojson }}
status: {{ status | lower }}
date: {{ date }}
{% if links %}links:
{% for link in links %}  - target: {{ link.target }}
    kind: {{ link.kind | lower | replace(" ", "") }}
{% endfor %}{% endif %}---

# {{ title }}

## Context and Problem Statement

{{ context if context else "결정이 필요한 상황과 제약을 적는다." }}

## Considered Options

- 검토한 대안을 적는다.

## Decision Outcome

{{ decision if decision else "선택한 대안과 그 이유를 적는다." }}

### Consequences

{{ consequences if consequences else "선택으로 생기는 이점과 감수할 제약을 적는다." }}
