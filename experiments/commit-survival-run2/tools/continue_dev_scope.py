"""Host driver: four isolated Docker workers; no model calls or Dev survival reruns."""
import concurrent.futures
import datetime
import json
from pathlib import Path
import subprocess
import time

EXP=Path(__file__).resolve().parents[1]
RESULTS=EXP/'results'
KTESTER=EXP.parents[1].parent/'KTester/docker'
TOOLS='/repo/experiments/commit-survival-run2/tools/'
EVENTS=RESULTS/'addendum4-events.json'

def event(stage,status,**extra):
    rows=json.loads(EVENTS.read_text()) if EVENTS.exists() else []
    rows.append(dict(stage=stage,status=status,time=datetime.datetime.now(datetime.timezone.utc).isoformat(),**extra))
    EVENTS.write_text(json.dumps(rows,indent=2)+'\n');print(json.dumps(rows[-1]),flush=True)

def completed(stage):
    return EVENTS.exists() and any(r['stage']==stage and r['status']=='completed' for r in json.loads(EVENTS.read_text()))

def invoke(label,script,args):
    command=['docker','compose','-f','compose.yml','-f','compose.commit-survival.yml','run','--rm','ktester','python',TOOLS+script,*args]
    with (RESULTS/f'addendum4-{label}-console.txt').open('a') as out:
        run=subprocess.run(command,cwd=KTESTER,stdout=out,stderr=subprocess.STDOUT)
    return run.returncode

def stage(name,script,*args):
    if completed(name):return
    begin=time.monotonic();event(name,'running')
    code=invoke(name,script,args)
    event(name,'completed' if code==0 else 'failed',seconds=round(time.monotonic()-begin,3),exit_code=code)
    assert code==0,name+' failed; inspect archived console'

def workers(name,script):
    if completed(name):return
    begin=time.monotonic();event(name,'running',workers=4,network_namespaces=4)
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        futures=[pool.submit(invoke,f'{name}-worker{i}',script,['--worker',str(i)]) for i in range(4)]
        codes=[f.result() for f in futures]
    event(name,'completed' if not any(codes) else 'failed',seconds=round(time.monotonic()-begin,3),exit_codes=codes)
    assert not any(codes),(name,codes)

if __name__=='__main__':
    stage('anchor-prepare','dev_method_coverage.py','--prepare')
    workers('anchor-methods','dev_method_coverage.py')
    stage('anchor-finalize','dev_method_coverage.py','--finalize')
    stage('derive-survival','derive_dev_scope.py')
    stage('survival-report','report_dev_scope.py','survival')
    workers('phase-b','run_dev_scope.py')
    stage('coverage-report','report_dev_scope.py','coverage')
    stage('verify','report_dev_scope.py','verify')
    print('Dev-scope measurements complete; final audit, documentation and commit remain.',flush=True)
