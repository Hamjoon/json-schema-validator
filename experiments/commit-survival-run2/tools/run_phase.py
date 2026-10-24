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
    parser=argparse.ArgumentParser();parser.add_argument('--phase',choices=['a','b'],required=True);parser.add_argument('--workers',type=int,default=4);parser.add_argument('--groups',nargs='+',choices=[*GROUPS,'ouedraogo-adj'],default=GROUPS);parser.add_argument('--result-suffix',default='');args=parser.parse_args()
    start=time.monotonic()
    commits=list(csv.DictReader((RESULTS/'commits.csv').open()))
    indices=[int(r['index']) for r in commits] if args.phase=='a' else [int(r['index']) for r in csv.DictReader((RESULTS/'coverage-commits.csv').open())]
    alive=json.loads((RESULTS/'alive-entering.json').read_text()) if args.phase=='b' else None
    jobs=[];skipped=[]
    for i in indices:
        if i and json.loads((RESULTS/f'builds/{i}.json').read_text())['build']!='ok':skipped.append(i);continue
        jobs.extend((i,g) for g in args.groups)
    def one(job):
        i,g=job
        try:
            result=run_group(i,g,args.phase,alive[str(i)][g] if alive is not None else None)
            killed=[run for f in result['files'] for run in [f['compile'],*f['runs']] if run.get('exit_code') in [-9,137]]
            if result.get('coverage_report',{}).get('exit_code') in [-9,137]:killed.append(result['coverage_report'])
            if result.get('whole_frozen_suite_compile',{}).get('exit_code') in [-9,137]:killed.append(result['whole_frozen_suite_compile'])
            print(json.dumps(dict(phase=args.phase,index=i,group=g,seconds=result['seconds'],states=__import__('collections').Counter(m['state'] for m in result['methods']))),flush=True)
            return dict(index=i,group=g,ok=result['status']=='evaluated',killed=bool(killed))
        except Exception:
            error=traceback.format_exc();write_json(RESULTS/f'phase-{args.phase}-errors/{i}-{g}.json',dict(error=error))
            print(json.dumps(dict(phase=args.phase,index=i,group=g,error=error)),flush=True)
            return dict(index=i,group=g,ok=False,killed=False)
    def bounded_batch(batch,workers):
        records=[];next_job=0;memory_failure=False
        with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as pool:
            pending=set()
            while pending or (next_job<len(batch) and not memory_failure):
                while not memory_failure and len(pending)<workers and next_job<len(batch):
                    pending.add(pool.submit(one,batch[next_job]));next_job+=1
                done,pending=concurrent.futures.wait(pending,return_when=concurrent.futures.FIRST_COMPLETED)
                for future in done:
                    result=future.result();records.append(result)
                    memory_failure=memory_failure or (workers>2 and result['killed'])
        return records,batch[next_job:]
    records,remaining=bounded_batch(jobs,args.workers)
    killed=[r for r in records if r['killed']]
    if killed:
        folder='raw' if args.phase=='a' else 'raw-coverage'
        for r in killed:
            path=RESULTS/f'{folder}/{r["index"]}-{r["group"]}.json'
            quarantine=RESULTS/f'quarantine/phase-{args.phase}/{path.name}';quarantine.parent.mkdir(parents=True,exist_ok=True);path.replace(quarantine)
        write_json(RESULTS/f'phase-{args.phase}{args.result_suffix}-oom.json',dict(quarantined=killed,resume_workers=2))
        retries,unfinished=bounded_batch([(r['index'],r['group']) for r in killed]+remaining,2)
        replacement={(r['index'],r['group']):r for r in retries}
        completed={(r['index'],r['group']):r for r in records};completed.update(replacement)
        records=list(completed.values())
        for r in retries:
            if r['killed']:
                r['ok']=False
                path=RESULTS/f'{folder}/{r["index"]}-{r["group"]}.json'
                quarantine=RESULTS/f'quarantine/phase-{args.phase}/second-{path.name}';path.replace(quarantine)
        records.extend(dict(index=i,group=g,ok=False,killed=False,reason='not started after repeated memory failure') for i,g in unfinished)
    write_json(RESULTS/f'phase-{args.phase}{args.result_suffix}.json',dict(seconds=round(time.monotonic()-start,3),workers=args.workers,resume_workers=2 if killed else None,skipped_build_fail=skipped,jobs=len(jobs),failed=[r for r in records if not r['ok']],oom_quarantine=killed))


if __name__=='__main__':main()
