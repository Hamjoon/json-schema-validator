"""Four isolated workers, atomic records, and explicit OOM quarantine/resume."""
import argparse
import concurrent.futures
import csv
import json
import time
import traceback
from start import EXP, RESULTS, write_json
from run_commit import GROUPS, run_group


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--phase',choices=['a','b'],required=True);parser.add_argument('--workers',type=int,default=4);args=parser.parse_args()
    start=time.monotonic()
    commits=list(csv.DictReader((RESULTS/'commits.csv').open()))
    indices=[int(r['index']) for r in commits] if args.phase=='a' else [int(r['index']) for r in csv.DictReader((RESULTS/'coverage-commits.csv').open())]
    alive=json.loads((RESULTS/'alive-entering.json').read_text()) if args.phase=='b' else None
    jobs=[];skipped=[]
    for i in indices:
        if i and json.loads((RESULTS/f'builds/{i}.json').read_text())['build']!='ok':skipped.append(i);continue
        jobs.extend((i,g) for g in GROUPS)
    def one(job):
        i,g=job
        try:
            result=run_group(i,g,args.phase,alive[str(i)][g] if alive is not None else None)
            killed=[run for f in result['files'] for run in [f['compile'],*f['runs']] if run.get('exit_code') in [-9,137]]
            if result.get('coverage_report',{}).get('exit_code') in [-9,137]:killed.append(result['coverage_report'])
            print(json.dumps(dict(phase=args.phase,index=i,group=g,seconds=result['seconds'],states=__import__('collections').Counter(m['state'] for m in result['methods']))),flush=True)
            return dict(index=i,group=g,ok=result['status']=='evaluated',killed=bool(killed))
        except Exception:
            error=traceback.format_exc();write_json(RESULTS/f'phase-{args.phase}-errors/{i}-{g}.json',dict(error=error))
            print(json.dumps(dict(phase=args.phase,index=i,group=g,error=error)),flush=True)
            return dict(index=i,group=g,ok=False,killed=False)
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as pool:
        records=list(pool.map(one,jobs))
    killed=[r for r in records if r['killed']]
    if killed:
        folder='raw' if args.phase=='a' else 'raw-coverage'
        for r in killed:
            path=RESULTS/f'{folder}/{r["index"]}-{r["group"]}.json'
            quarantine=RESULTS/f'quarantine/phase-{args.phase}/{path.name}';quarantine.parent.mkdir(parents=True,exist_ok=True);path.replace(quarantine)
        write_json(RESULTS/f'phase-{args.phase}-oom.json',dict(quarantined=killed,resume_workers=2))
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
            retries=list(pool.map(one,[(r['index'],r['group']) for r in killed]))
        replacement={(r['index'],r['group']):r for r in retries}
        records=[replacement.get((r['index'],r['group']),r) for r in records]
    write_json(RESULTS/f'phase-{args.phase}.json',dict(seconds=round(time.monotonic()-start,3),workers=args.workers,resume_workers=2 if killed else None,skipped_build_fail=skipped,jobs=len(jobs),failed=[r for r in records if not r['ok']],oom_quarantine=killed))


if __name__=='__main__':main()
