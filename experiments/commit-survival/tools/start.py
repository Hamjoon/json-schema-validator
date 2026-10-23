"""Run the prescribed anchor build inside the KTester container, retaining evidence."""
import datetime
import json
import os
from pathlib import Path
import signal
import subprocess
import time

EXP = Path(__file__).resolve().parents[1]
REPO = EXP.parents[1]
RESULTS = EXP / 'results'


def git(*args):
    return subprocess.check_output(['git', '-C', str(REPO), *args], text=True).strip()


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary=path.with_name(path.name+'.tmp')
    temporary.write_text(json.dumps(value, indent=2) + '\n')
    temporary.replace(path)


def execute(command, cwd, log, timeout=600):
    start = time.monotonic()
    log.parent.mkdir(parents=True, exist_ok=True)
    with log.open('w') as stream:
        child = subprocess.Popen(command, cwd=cwd, stdout=stream, stderr=subprocess.STDOUT, start_new_session=True)
        timed_out = False
        try:
            code = child.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            timed_out = True
            os.killpg(child.pid, signal.SIGKILL)
            code = child.wait()
    return dict(command=command, cwd=str(cwd), exit_code=code, timeout=timed_out,
                seconds=round(time.monotonic()-start, 3), log=str(log.relative_to(EXP)))


def main():
    assert Path('/work/code').is_dir(), 'Run inside the KTester container'
    anchor = dict(tag='1.1.0', sha=git('rev-parse', '1.1.0^{commit}'),
                  origin_master=git('rev-parse', 'origin/master'),
                  n=int(git('rev-list', '--count', '--first-parent', '1.1.0..origin/master')),
                  started=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                  deviations=['Tag 1.1.0 was absent locally and fetched from networknt; upstream remote was absent.'],
                  key_check='KEY_OK (value never logged)',
                  ktester_base=subprocess.check_output(['git', '-C', '/work', 'rev-parse', 'HEAD'], text=True).strip())
    write_json(RESULTS / 'anchor.json', anchor)
    environment = execute(['bash', '-c', 'java -version; mvn -version'], REPO, RESULTS / 'environment.txt')
    write_json(RESULTS / 'environment.json', environment)
    attempts = []
    for attempt in (1, 2):
        if attempt == 2:
            print('Anchor build failed; retrying after 60 seconds', flush=True)
            time.sleep(60)
        attempts.append(execute(['mvn', '-q', '-DskipTests', 'package'], REPO, RESULTS / f'build-at-t-attempt{attempt}.txt'))
        write_json(RESULTS / 'build-at-t.json', dict(attempts=attempts, success=attempts[-1]['exit_code']==0))
        print(json.dumps(attempts[-1]), flush=True)
        if attempts[-1]['exit_code'] == 0:
            break
    success = attempts[-1]['exit_code'] == 0
    (RESULTS / 'build-at-t.md').write_text('# Anchor build\n\nJDK 17, `mvn -q -DskipTests package`, inside KTester. No author files modified.\n\n' + '\n'.join(f"- Attempt {i}: exit {r['exit_code']}, {r['seconds']} seconds; `{r['log']}`." for i,r in enumerate(attempts, 1)) + '\n')
    (RESULTS / 'STATUS.md').write_text('# Experiment status\n\n' + ('Anchor build passed; Step 1 continues.\n' if success else 'STOP: Step 1.2 failed twice, triggering the mandatory stop condition. Later steps were not run.\n'))
    if not success:
        handover = '# Step 1 handover\n\nMandatory stop: anchor package build failed twice. See results/build-at-t.json and attempt logs. No tests or model calls run. No Phase A/B workers, OOMs, or quarantine events.\n'
        (EXP / 'docs/handover-part1.md').write_text(handover)
        (EXP / 'docs/handover-final.md').write_text(handover)
        raise SystemExit(2)


if __name__ == '__main__':
    main()
