#!/usr/bin/env python3
"""Send one immutable deployment, wait for final status, never print remote logs."""
import json
import os
import re
import subprocess
import time


def aws(*args):
    result = subprocess.run(['aws', 'ssm', *args, '--output', 'json'], capture_output=True, text=True)
    if result.returncode:
        # Invocation registration is eventually consistent; other failures must surface.
        if 'InvocationDoesNotExist' in result.stderr:
            return None
        raise RuntimeError('SSM API failed; check target, role and region (remote output suppressed).')
    return json.loads(result.stdout)


def main():
    instance = os.environ['INSTANCE_ID']
    sha = os.environ['GITHUB_SHA']
    repo = os.environ['GITHUB_REPOSITORY']
    image = os.environ['BACKEND_IMAGE']
    if not re.fullmatch(r'i-[0-9a-f]{17}', instance):
        raise ValueError('Invalid instance ID')
    if not re.fullmatch(r'[0-9a-f]{40}', sha) or not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', repo):
        raise ValueError('Invalid release source')
    if not re.fullmatch(re.escape('ghcr.io/' + repo.lower()) + r'@sha256:[0-9a-f]{64}', image):
        raise ValueError('Image must belong to this repository and use a digest')
    # Repository is public. No credentials or .env contents enter SSM command history.
    command = f'''set -eu
umask 077
test -f /opt/daiso/.env
mkdir -p /opt/daiso/releases
release=/opt/daiso/releases/{sha}
if [ ! -d "$release" ]; then
  stage=$(mktemp -d /opt/daiso/releases/.stage-XXXXXXXX)
  trap 'rm -rf "$stage"' EXIT
  curl --fail --silent --show-error --location --max-time 120 https://api.github.com/repos/{repo}/tarball/{sha} -o "$stage/source.tar.gz"
  mkdir "$stage/source"
  tar -xzf "$stage/source.tar.gz" -C "$stage/source" --strip-components=1
  mv "$stage/source" "$release"
fi
bash "$release/scripts/deploy.sh" '{image}'
'''
    response = aws('send-command', '--instance-ids', instance, '--document-name', 'AWS-RunShellScript',
                   '--timeout-seconds', '120', '--parameters', json.dumps({'commands': [command], 'executionTimeout': ['1500']}))
    command_id = response['Command']['CommandId']
    print(f'SSM command: {command_id}', flush=True)
    deadline = time.monotonic() + 1650
    while time.monotonic() < deadline:
        result = aws('get-command-invocation', '--command-id', command_id, '--instance-id', instance)
        if result and result['Status'] not in ('Pending', 'InProgress', 'Delayed', 'Cancelling'):
            if result['Status'] == 'Success' and result['ResponseCode'] == 0:
                print('SSM deployment completed.')
                return
            raise RuntimeError(f"SSM deployment failed: {result['Status']}; inspect command {command_id}.")
        time.sleep(10)
    raise TimeoutError(f'SSM completion unconfirmed: {command_id}; check before retrying.')


if __name__ == '__main__':
    main()
