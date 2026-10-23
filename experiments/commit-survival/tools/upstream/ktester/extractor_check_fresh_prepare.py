#!/usr/bin/env python3
"""Prepare the pinned upstream checkout without copying author snapshot files."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'results/extractor-check'
DS = ROOT / 'scratch/extractor-check/cli-fresh'
PROJECT = DS / 'projects/commons-cli'
COMMIT = '58c64b52a8fb1c38c4f4b2d41db0349241475f95'


def main():
    start = time.monotonic()
    PROJECT.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(['git', 'clone', 'https://github.com/apache/commons-cli.git', str(PROJECT)], check=True)
    subprocess.run(['git', 'checkout', '--detach', COMMIT], cwd=PROJECT, check=True)
    actual = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=PROJECT, text=True).strip()
    assert actual == COMMIT
    attempts = []
    command = ['mvn', '-q', 'dependency:copy-dependencies', '-DoutputDirectory=libs']
    for attempt in range(1, 3):
        log = OUT / f'fresh-maven-attempt-{attempt}.txt'
        t = time.monotonic()
        with log.open('w') as stream:
            cp = subprocess.run(command, cwd=PROJECT, stdout=stream, stderr=subprocess.STDOUT)
        attempts.append({'attempt': attempt, 'command': command, 'exit_code': cp.returncode,
                         'seconds': round(time.monotonic() - t, 3), 'log': str(log.relative_to(ROOT))})
        print(json.dumps(attempts[-1]), flush=True)
        if cp.returncode == 0:
            break
    result = {'upstream': 'https://github.com/apache/commons-cli.git', 'commit': actual,
              'maven_attempts': attempts, 'status': 'prepared' if cp.returncode == 0 else 'stopped_maven_failed'}
    if cp.returncode == 0:
        jars = sorted((PROJECT / 'libs').glob('*.jar'))
        assert jars, 'Maven returned success without dependency jars'
        (PROJECT / 'dependencies.txt').write_text('target/classes:' + ':'.join('libs/' + p.name for p in jars))
        shutil.copytree(PROJECT / 'src/test', PROJECT / 'src/test-original')
        # build_calling_graph uses project keys only; DatasetPrepare replaces this bootstrap metadata.
        (DS / 'dataset_info.json').write_text(json.dumps({'commons-cli': {
            'project-name': 'commons-cli', 'project-url': 'projects/commons-cli', 'focal-methods': []}}, indent=2) + '\n')
        result['dependencies_txt'] = (PROJECT / 'dependencies.txt').read_text()
        result['jars_sha256'] = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in jars}
        result['jar_count'] = len(jars)
    result['seconds'] = round(time.monotonic() - start, 3)
    (OUT / 'fresh-prepare.json').write_text(json.dumps(result, indent=2) + '\n')
    if cp.returncode:
        raise SystemExit('STOP: fresh Maven dependency build failed after one retry')


if __name__ == '__main__':
    main()
