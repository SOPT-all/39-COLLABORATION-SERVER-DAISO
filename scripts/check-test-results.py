#!/usr/bin/env python3
"""CI must not publish a build with missing, failed or skipped tests."""
from pathlib import Path
import xml.etree.ElementTree as ET

files = list(Path('build/test-results/test').glob('TEST-*.xml'))
assert files, 'No test reports produced'
count = 0
for path in files:
    suite = ET.parse(path).getroot()
    count += int(suite.attrib['tests'])
    for key in ('failures', 'errors', 'skipped'):
        assert int(suite.attrib.get(key, 0)) == 0, f'{path.name}: {key}'
assert count > 0, 'No tests executed'
print(f'{count} tests passed, none skipped')
