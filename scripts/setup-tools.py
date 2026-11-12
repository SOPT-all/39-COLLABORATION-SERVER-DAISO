#!/usr/bin/env python3
"""프로젝트 전용 adrs 실행 파일을 설치한다."""
import hashlib
import io
import platform
from pathlib import Path
import subprocess
import tarfile
import urllib.request

VERSION = "0.13.0"
ASSETS = {
    ("Darwin", "arm64"): ("aarch64-apple-darwin", "16af3d067cfcd4c537c925c80751a318a88261d96d33757700d538791edf9e5d"),
    ("Darwin", "x86_64"): ("x86_64-apple-darwin", "c68b8ddd2c99e40a11d76950cf531c589cd5dcbf6dba431cf6585255c0037749"),
    ("Linux", "x86_64"): ("x86_64-unknown-linux-gnu", "15dee3fb41f22d1e511ad3bfe3ca03969bb8259ba7ce6f1b8040cba7c0ec8b04"),
    ("Linux", "aarch64"): ("aarch64-unknown-linux-gnu", "d006a2d78e0de24750e4b0a03ad973e6eab61120af684222dcc4acb0089b44ac"),
}
binary = Path(__file__).resolve().parent.parent / ".tools/bin/adrs"
if binary.exists():
    result = subprocess.run([str(binary), "--version"], capture_output=True, text=True, check=True)
    if result.stdout.strip() == f"adrs {VERSION}":
        print(result.stdout.strip() + " 준비됨")
        raise SystemExit(0)

asset = ASSETS.get((platform.system(), platform.machine()))
if asset is None:
    raise SystemExit("지원되는 설치 환경: macOS 또는 Linux의 arm64/x86_64")
target, expected_hash = asset
url = f"https://github.com/joshrotenberg/adrs/releases/download/v{VERSION}/adrs-{target}.tar.xz"
with urllib.request.urlopen(url, timeout=60) as response:
    archive = response.read()
if hashlib.sha256(archive).hexdigest() != expected_hash:
    raise SystemExit("adrs 다운로드의 SHA-256이 일치하지 않습니다.")
with tarfile.open(fileobj=io.BytesIO(archive), mode="r:xz") as package:
    member = next(m for m in package.getmembers() if Path(m.name).name == "adrs" and m.isfile())
    source = package.extractfile(member)
    binary.parent.mkdir(parents=True, exist_ok=True)
    binary.write_bytes(source.read())
binary.chmod(0o755)
subprocess.run([str(binary), "--version"], check=True)
