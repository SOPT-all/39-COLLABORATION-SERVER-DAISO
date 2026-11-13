"""No AWS calls: verify SSM target validation and final-result handling."""
import importlib.util
import os
import tempfile
import subprocess
import json
import shutil
import unittest
from unittest.mock import patch
from pathlib import Path

spec = importlib.util.spec_from_file_location('deploy_ssm', Path(__file__).resolve().parents[1] / 'scripts/deploy-ssm.py')
ssm = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ssm)


class SsmDeploymentTest(unittest.TestCase):
    def setUp(self):
        self.env = patch.dict(os.environ, INSTANCE_ID='i-' + '1' * 17, GITHUB_SHA='a' * 40,
                              GITHUB_REPOSITORY='SOPT-all/example',
                              BACKEND_IMAGE='ghcr.io/sopt-all/example@sha256:' + 'b' * 64)
        self.env.start()
        self.addCleanup(self.env.stop)

    def test_waits_until_success_and_uses_no_secret(self):
        with patch.object(ssm, 'aws', side_effect=[{'Command': {'CommandId': 'cmd'}}, None,
                {'Status': 'InProgress'}, {'Status': 'Success', 'ResponseCode': 0}]) as call, patch.object(ssm.time, 'sleep'):
            ssm.main()
        self.assertEqual(call.call_count, 4)
        command = ' '.join(call.call_args_list[0].args)
        self.assertNotIn('password', command.lower())
        self.assertIn('a' * 40, command)
        self.assertNotIn('/main', command)

    def test_failure_or_nonzero_success_is_failure(self):
        for status, code in [('Failed', 1), ('TimedOut', -1), ('Cancelled', -1), ('Success', 1)]:
            with self.subTest(status=status), patch.object(ssm, 'aws', side_effect=[
                    {'Command': {'CommandId': 'cmd'}}, {'Status': status, 'ResponseCode': code}]), self.assertRaises(RuntimeError):
                ssm.main()

    def test_timeout_is_not_success(self):
        with patch.object(ssm, 'aws', return_value={'Command': {'CommandId': 'cmd'}}), \
                patch.object(ssm.time, 'monotonic', side_effect=[0, 2000]), self.assertRaises(TimeoutError):
            ssm.main()

    def test_bad_target_or_image_never_sends(self):
        for key, value in [('INSTANCE_ID', 'all'), ('GITHUB_SHA', 'main'),
                           ('BACKEND_IMAGE', 'ghcr.io/other/repo@sha256:' + 'b' * 64)]:
            with self.subTest(key=key), patch.dict(os.environ, {key: value}), patch.object(ssm, 'aws') as call:
                with self.assertRaises(ValueError):
                    ssm.main()
                call.assert_not_called()


class DeployScriptTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.base = self.root / 'server'
        (self.base / 'state').mkdir(parents=True)
        (self.base / '.env').write_text('MYSQL_PASSWORD=not-for-logs\n')
        (self.base / '.env').chmod(0o600)
        self.old = self.release('old', 'unchanged')
        self.new = self.release('new', 'unchanged')
        self.old_image = 'ghcr.io/test/app@sha256:' + 'a' * 64
        self.new_image = 'ghcr.io/test/app@sha256:' + 'b' * 64
        (self.base / 'state/current').write_text(str(self.old))
        (self.base / 'state/image').write_text(self.old_image)
        tools = self.root / 'bin'
        tools.mkdir()
        docker = tools / 'docker'
        docker.write_text("""#!/usr/bin/env python3
import os,sys,json
args=sys.argv[1:]
with open(os.environ['CALLS'],'a') as f: f.write(json.dumps([args,os.environ.get('BACKEND_IMAGE')])+'\\n')
if args[0]=='pull' and os.environ.get('FAIL_PULL'): sys.exit(1)
if args[:2]==['image','inspect']: print('arm64')
if 'exec' in args and os.environ.get('UNKNOWN_DB'): sys.exit(1)
if 'up' in args and args[-1]=='backend' and os.environ.get('FAIL_NEW') and os.environ['BACKEND_IMAGE'].endswith('b'*64): sys.exit(1)
""")
        docker.chmod(0o755)
        # macOS lacks GNU stat/flock/sha256sum; shell behavior tests run in Linux below.
        self.env = dict(os.environ, PATH=str(tools)+':'+os.environ['PATH'],
                        DEPLOY_ROOT=str(self.base), CALLS=str(self.root/'calls'))

    def release(self, name, sql):
        folder = self.root / name
        (folder / 'scripts').mkdir(parents=True)
        migrations = folder / 'src/main/resources/db/migration'
        migrations.mkdir(parents=True)
        (migrations / 'V1.sql').write_text(sql)
        (folder / 'docker-compose.prod.yml').write_text('services: {}')
        shutil.copy(Path(__file__).resolve().parents[1] / 'scripts/deploy.sh', folder / 'scripts/deploy.sh')
        return folder

    def run_deploy(self, **flags):
        result = subprocess.run(['bash',str(self.new/'scripts/deploy.sh'),self.new_image],
                                env=dict(self.env,**flags),capture_output=True,text=True)
        self.assertNotIn('not-for-logs',result.stdout+result.stderr)
        calls = [json.loads(line) for line in (self.root/'calls').read_text().splitlines()]
        self.assertFalse(any('down' in args or 'prune' in args for args,_ in calls))
        return result,calls

    @unittest.skipUnless(os.uname().sysname == 'Linux', 'Run shell tests in Linux container')
    def test_success_records_release(self):
        result,_ = self.run_deploy()
        self.assertEqual(result.returncode,0,result.stderr)
        self.assertEqual((self.base/'state/image').read_text().strip(),self.new_image)

    @unittest.skipUnless(os.uname().sysname == 'Linux', 'Run shell tests in Linux container')
    def test_pull_failure_never_replaces_running_app(self):
        result,calls = self.run_deploy(FAIL_PULL='1')
        self.assertNotEqual(result.returncode,0)
        self.assertFalse(any('up' in args for args,_ in calls))

    @unittest.skipUnless(os.uname().sysname == 'Linux', 'Run shell tests in Linux container')
    def test_app_failure_restores_previous_but_stays_failed(self):
        result,calls = self.run_deploy(FAIL_NEW='1')
        self.assertNotEqual(result.returncode,0)
        self.assertTrue(any('up' in args and args[-1]=='backend' and image==self.old_image for args,image in calls))
        self.assertEqual((self.base/'state/image').read_text(),self.old_image)

    @unittest.skipUnless(os.uname().sysname == 'Linux', 'Run shell tests in Linux container')
    def test_unknown_db_never_auto_restores(self):
        result,calls = self.run_deploy(FAIL_NEW='1', UNKNOWN_DB='1')
        self.assertNotEqual(result.returncode,0)
        self.assertFalse(any('up' in args and args[-1]=='backend' and image==self.old_image for args,image in calls))

    @unittest.skipUnless(os.uname().sysname == 'Linux', 'Run shell tests in Linux container')
    def test_missing_migration_sources_never_auto_restores(self):
        for release in (self.old,self.new):
            shutil.rmtree(release/'src/main/resources/db/migration')
        result,calls = self.run_deploy(FAIL_NEW='1')
        self.assertNotEqual(result.returncode,0)
        self.assertFalse(any('up' in args and args[-1]=='backend' and image==self.old_image for args,image in calls))

    @unittest.skipUnless(os.uname().sysname == 'Linux', 'Run shell tests in Linux container')
    def test_first_or_migration_change_never_auto_restores(self):
        for first in (False,True):
            with self.subTest(first=first):
                (self.new/'src/main/resources/db/migration/V1.sql').write_text('changed')
                if first: (self.base/'state/current').unlink()
                (self.root/'calls').write_text('')
                result,calls = self.run_deploy(FAIL_NEW='1')
                self.assertNotEqual(result.returncode,0)
                self.assertFalse(any('up' in args and args[-1]=='backend' and image==self.old_image for args,image in calls))


if __name__ == '__main__':
    unittest.main()
