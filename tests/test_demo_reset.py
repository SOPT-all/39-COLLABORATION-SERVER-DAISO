"""Real MySQL transaction checks in a disposable container; never uses a project DB."""
import subprocess
import time
import unittest
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class DemoResetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.container = 'daiso-reset-test-' + uuid.uuid4().hex[:12]
        subprocess.run(['docker', 'run', '--detach', '--rm', '--name', cls.container,
                        '--tmpfs', '/var/lib/mysql', '-e', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes',
                        'mysql:8.4'], check=True, capture_output=True)
        cls.addClassCleanup(lambda: subprocess.run(['docker', 'rm', '-f', cls.container],
                                                  check=True, capture_output=True))
        for _ in range(90):
            result = cls.query('SELECT 1;', check=False, database=False)
            if result.returncode == 0:
                break
            time.sleep(1)
        else:
            raise RuntimeError('Disposable MySQL did not become ready')
        cls.query('CREATE DATABASE reset_test;', database=False)
        for migration in sorted((ROOT / 'src/main/resources/db/migration').glob('V*.sql')):
            cls.query(migration.read_text())
        cls.query('CREATE TABLE flyway_schema_history (version INT); INSERT INTO flyway_schema_history VALUES (3);')
        cls.reset_sql = subprocess.run(['bash', str(ROOT / 'scripts/render-reset-sql.sh')],
                                       check=True, capture_output=True, text=True).stdout

    @classmethod
    def query(cls, sql, check=True, database=True):
        result = subprocess.run(['docker', 'exec', '-i', cls.container, 'mysql', '-uroot', '--protocol=TCP', '-h127.0.0.1',
                               '--batch', '--skip-column-names', '--default-character-set=utf8mb4']
                              + (['reset_test'] if database else []), input=sql, text=True,
                              capture_output=True)
        if check and result.returncode:
            raise AssertionError(result.stderr)
        return result

    def snapshot(self):
        return self.query('SELECT * FROM product ORDER BY id; SELECT * FROM store ORDER BY id; '
                          'SELECT * FROM store_inventory ORDER BY id; SELECT * FROM flyway_schema_history;').stdout

    def test_repeat_reset_and_failed_insert_rollback(self):
        self.query(self.reset_sql)
        baseline = self.snapshot()
        self.assertEqual(self.query('SELECT (SELECT COUNT(*) FROM product), '
                                   '(SELECT COUNT(*) FROM store), (SELECT COUNT(*) FROM store_inventory);').stdout.strip(),
                         '15\t8\t120')
        self.query('UPDATE product SET is_liked = NOT is_liked; UPDATE store SET is_favorite = NOT is_favorite; '
                   'UPDATE store_inventory SET stock_quantity = 999;')
        self.query(self.reset_sql)
        self.assertEqual(self.snapshot(), baseline)
        self.query('UPDATE product SET price = 42 WHERE id=1;')
        before_failure = self.snapshot()
        broken = self.reset_sql.replace('COMMIT;', 'INSERT INTO product SELECT * FROM product WHERE id=1;\nCOMMIT;')
        self.assertNotEqual(self.query(broken, check=False).returncode, 0)
        self.assertEqual(self.snapshot(), before_failure)


if __name__ == '__main__':
    unittest.main()
