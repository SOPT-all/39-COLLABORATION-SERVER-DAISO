"""설치된 도구의 실제 ADR 생성·상태 관리와 MCP 조회를 임시 저장소에서 검증한다."""
import json
import re
from pathlib import Path
import select
import shutil
import subprocess
import tempfile
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
BINARY = ROOT / '.tools/bin/adrs'

def run(*args, cwd):
    return subprocess.run([str(BINARY), *args], cwd=cwd, check=True, capture_output=True, text=True).stdout

with tempfile.TemporaryDirectory(prefix='daiso-harness-') as directory:
    work = Path(directory)
    (work / 'docs').mkdir()
    shutil.copytree(ROOT / 'docs/adr', work / 'docs/adr')
    shutil.copy(ROOT / 'adrs.toml', work / 'adrs.toml')
    next_number = max(int(p.name.split('-')[0]) for p in (work / 'docs/adr').glob('[0-9]*-*.md')) + 1
    run('new', '하네스 검증용 결정', cwd=work)
    records = list((work / 'docs/adr').glob(f'{next_number:04d}-*.md'))
    assert len(records) == 1, '기존 ADR 다음 번호로 생성해야 한다'
    text = records[0].read_text()
    assert 'status: proposed' in text and '## Decision Outcome' in text
    run('status', str(next_number), 'accepted', cwd=work)
    assert 'status: accepted' in records[0].read_text()
    # A non-ADR AGENTS/template/README must not become a record.
    listing = run('list', cwd=work)
    assert '0002-' in listing and f'{next_number:04d}-' in listing
    assert 'AGENTS.md' not in listing and 'template.md' not in listing
    server = subprocess.Popen([str(BINARY), 'mcp', 'serve'], cwd=work,
                              stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                              stderr=subprocess.DEVNULL, text=True, bufsize=1)
    try:
        def rpc(method, params, identifier):
            server.stdin.write(json.dumps({'jsonrpc': '2.0', 'id': identifier,
                                           'method': method, 'params': params}) + '\n')
            server.stdin.flush()
            assert select.select([server.stdout], [], [], 10)[0], 'MCP 응답 시간 초과'
            response = json.loads(server.stdout.readline())
            assert response.get('id') == identifier and 'error' not in response, response
            return response['result']
        rpc('initialize', {'protocolVersion': '2024-11-05', 'capabilities': {},
                           'clientInfo': {'name': 'daiso-harness-check', 'version': '1'}}, 1)
        server.stdin.write(json.dumps({'jsonrpc': '2.0', 'method': 'notifications/initialized'}) + '\n')
        server.stdin.flush()
        tools = rpc('tools/list', {}, 2)['tools']
        names = {t['name'] for t in tools}
        assert {'list_adrs', 'get_adr', 'create_adr', 'update_status'} <= names, names
        result = rpc('tools/call', {'name': 'get_adr', 'arguments': {'number': 2}}, 3)
        assert not result.get('isError'), result
        source = next((work / 'docs/adr').glob('0002-*.md')).read_text()
        expected_status = re.search(r'^status:\s*(\S+)', source.split('---', 2)[1], re.M).group(1).strip("\"'")
        returned_adr = json.loads(result['content'][0]['text'])
        assert returned_adr['status'].lower() == expected_status.lower(), result
        print('ADR 생성·상태 변경, MCP 초기화·도구 조회·ADR 0002 조회: OK')
    finally:
        server.terminate()
        server.wait(timeout=5)

def heading_anchors(content):
    content = re.sub(r'```.*?```', '', content, flags=re.S)
    anchors = set()
    for heading in re.findall(r'^ {0,3}#{1,6}\s+(.+?)\s*#*$', content, re.M):
        heading = re.sub(r'\[([^]]+)\]\([^)]+\)', r'\1', heading)
        base = re.sub(r'[^\w\- ]', '', heading.lower()).replace(' ', '-')
        anchor, suffix = base, 0
        while anchor in anchors:
            suffix += 1
            anchor = f'{base}-{suffix}'
        anchors.add(anchor)
    return anchors


def check_local_links(page):
    content = re.sub(r'```.*?```', '', page.read_text(), flags=re.S)
    for link in re.findall(r'\]\(([^)]+)\)', content):
        if '{{' in link:
            continue
        url = urlsplit(link)
        if url.scheme or url.netloc:
            continue
        target = page.parent / unquote(url.path) if url.path else page
        assert target.exists(), (page, link)
        if url.fragment:
            assert target.is_file() and unquote(url.fragment) in heading_anchors(target.read_text()), (page, link)


for page in [ROOT / 'AGENTS.md', ROOT / 'README.md', * (ROOT / 'docs').rglob('*.md'), ROOT / 'openspec/AGENTS.md']:
    check_local_links(page)
print('작업 지침·개발 문서의 로컬 파일·내부 절 링크: OK')

with tempfile.TemporaryDirectory(prefix='daiso-openspec-') as directory:
    work = Path(directory)
    (work / 'openspec/specs/harness-check').mkdir(parents=True)
    shutil.copy(ROOT / 'openspec/config.yaml', work / 'openspec/config.yaml')
    (work / 'openspec/specs/harness-check/spec.md').write_text(
        '# Harness check\n\n## Purpose\n\nValidate OpenSpec integration in a disposable fixture.\n\n'
        '## Requirements\n\n### Requirement: Check tool configuration\n'
        'The harness SHALL validate this temporary specification.\n\n'
        '#### Scenario: Validate a fixture\n- **WHEN** validation runs\n'
        '- **THEN** the temporary specification passes validation\n')
    cli = ROOT / 'node_modules/@fission-ai/openspec/bin/openspec.js'
    subprocess.run(['node', str(cli), 'validate', '--all', '--strict', '--no-interactive'],
                   cwd=work, check=True)
print('OpenSpec 설정·임시 기능 명세 strict 검증: OK')
