"""Freeze the complete first-parent range and build each commit, resumably."""
import concurrent.futures
import csv
import hashlib
import io
import json
import shutil
import subprocess
import tarfile
import threading
import time
from start import EXP, REPO, RESULTS, execute, git, write_json

LOCK = threading.Lock()


def freeze():
    anchor = json.loads((RESULTS / 'anchor.json').read_text())
    shas = git('rev-list', '--first-parent', '--reverse', anchor['sha']+'..'+anchor['origin_master']).splitlines()
    rows = []
    touches = {}
    for index, sha in enumerate(shas, 1):
        files = git('diff', '--no-renames', '--name-only', sha+'^1', sha).splitlines()
        touches[str(index)] = files
        date, subject = git('show', '-s', '--format=%cI%n%s', sha).split('\n', 1)
        rows.append(dict(index=index, sha=sha, date=date, subject=subject,
                         **{'changes-src-main': 'yes' if any(p.startswith('src/main/java/') for p in files) else 'no',
                            'changes-pom': 'yes' if 'pom.xml' in files else 'no'}))
    with (RESULTS / 'commits.csv').open('w', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]), lineterminator='\n')
        writer.writeheader(); writer.writerows(rows)
    write_json(RESULTS / 'commit-paths.json', touches)
    assert len(rows) == anchor['n']
    return rows


def build(row):
    index = row['index']
    record = RESULTS / f'builds/{index}.json'
    if record.exists():
        return json.loads(record.read_text())
    started = time.monotonic()
    dest = EXP / f'checkouts/{index}'
    dest.mkdir(parents=True, exist_ok=True)
    archive = subprocess.check_output(['git', '-C', str(REPO), 'archive', row['sha']])
    with tarfile.open(fileobj=io.BytesIO(archive)) as stream:
        stream.extractall(dest, filter='data')
    digest = hashlib.sha256((dest / 'pom.xml').read_bytes()).hexdigest()
    attempts = []
    for attempt in (1, 2):
        if attempt == 2:
            time.sleep(60)
        command = ['mvn', '-q', *(['-o'] if attempt == 1 else []), '-DskipTests', 'compile']
        run = execute(command, dest, RESULTS / f'builds/logs/{index}-{attempt}.txt')
        attempts.append(run)
        if run['exit_code'] == 0:
            break
    ok = attempts[-1]['exit_code'] == 0
    cp_runs = []
    cache = EXP / f'work/classpaths/{digest}.txt'
    if ok:
        # A single dependency resolution per distinct pom, shared by all commits.
        with LOCK:
            if not cache.exists():
                cache.parent.mkdir(parents=True, exist_ok=True)
                for attempt in (1, 2):
                    if attempt == 2:
                        time.sleep(60)
                    command = ['mvn', '-q', *(['-o'] if attempt == 1 else []), 'dependency:build-classpath', '-DincludeScope=test', '-Dmdep.outputFile='+str(cache)]
                    cp_runs.append(execute(command, dest, RESULTS / f'builds/logs/classpath-{digest}-{attempt}.txt'))
                    if cp_runs[-1]['exit_code'] == 0 and cache.exists():
                        break
    result = dict(index=index, sha=row['sha'], build='ok' if ok else 'fail', attempts=attempts,
                  pom_sha256=digest, classpath=str(cache.relative_to(EXP)) if cache.exists() else None,
                  classpath_attempts=cp_runs, seconds=round(time.monotonic()-started,3))
    write_json(record, result)
    print(json.dumps({k:result[k] for k in ['index','build','seconds']}), flush=True)
    return result


def main():
    started = time.monotonic()
    rows = freeze()
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
        results = list(executor.map(build, rows))
    write_json(RESULTS / 'build-check.json', dict(workers=4, seconds=round(time.monotonic()-started,3),
              ok=sum(r['build']=='ok' for r in results), failed=[r['index'] for r in results if r['build']=='fail']))
    (RESULTS / 'commits-check.md').write_text('# Complete range build check\n\n' + f'{len(rows)} first-parent commits; {sum(r["build"]=="ok" for r in results)} built successfully. Build failures carry forward survival and are excluded from evaluation.\n\n| Index | Build | Seconds |\n|---:|---|---:|\n' + '\n'.join(f'| {r["index"]} | {r["build"]} | {r["seconds"]} |' for r in results)+'\n')


if __name__ == '__main__':
    main()
