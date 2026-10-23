"""Freeze anchor tests and run three complete Maven baselines with 600s limits."""
import collections
import hashlib
import json
import shutil
import time
import xml.etree.ElementTree as ET
from start import EXP, REPO, RESULTS, execute, write_json


def main():
    assert json.loads((RESULTS / 'build-at-t.json').read_text())['success']
    start = time.monotonic()
    frozen = EXP / 'dev/src-test-t'
    for name in ['java', 'resources', 'suite']:
        source = REPO / 'src/test' / name
        if source.exists():
            shutil.copytree(source, frozen / name, dirs_exist_ok=True)
    write_json(EXP / 'dev/frozen-source-sha256.json', {str(p.relative_to(frozen)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(frozen.rglob('*')) if p.is_file()})
    runs = []
    for index in range(1, 4):
        record_path = RESULTS / f'baseline-run{index}.json'
        if record_path.exists():
            runs.append(json.loads(record_path.read_text()))
            continue
        reports = REPO / 'target/surefire-reports'
        if reports.exists():
            shutil.rmtree(reports)
        attempts = []
        for attempt in (1, 2):
            if attempt == 2:
                time.sleep(60)
            command = ['mvn', '-q', *(['-o'] if attempt == 1 else []), 'test']
            log = RESULTS / (f'baseline-run{index}.txt' if attempt == 1 else f'baseline-run{index}-retry.txt')
            run = execute(command, REPO, log)
            attempts.append(run)
            if run['exit_code'] == 0 or reports.exists():
                break
        destination = RESULTS / f'baseline-reports/run{index}'
        if reports.exists():
            shutil.copytree(reports, destination, dirs_exist_ok=True)
        totals = collections.Counter()
        for p in destination.glob('TEST-*.xml'):
            root = ET.parse(p).getroot()
            for key in ['tests', 'failures', 'errors', 'skipped']:
                totals[key] += int(root.get(key, 0))
        totals['passed'] = totals['tests']-totals['failures']-totals['errors']-totals['skipped']
        record = dict(index=index, attempts=attempts, totals=dict(totals))
        write_json(record_path, record)
        with (RESULTS / f'baseline-run{index}.txt').open('a') as out:
            out.write('\nRecorded counts: ' + json.dumps(dict(totals)) + '\n')
        runs.append(record)
        print(json.dumps(record), flush=True)
    write_json(RESULTS / 'developer-baseline.json', dict(runs=runs, seconds=round(time.monotonic()-start,3)))


if __name__ == '__main__':
    main()
