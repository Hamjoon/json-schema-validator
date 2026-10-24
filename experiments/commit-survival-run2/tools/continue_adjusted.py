"""Container-only, resumable addendum stages; never regenerate or retry model calls."""
import collections
import csv
import datetime
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import time
from adjusted_prompts import BASE
from start import EXP,RESULTS,write_json
from run_commit import denominator

GROUP='ouedraogo-adj'
EVENTS=RESULTS/'addendum2-events.json'

def stage(name,script,*args):
    events=json.loads(EVENTS.read_text()) if EVENTS.exists() else []
    if any(r['stage']==name and r['status']=='completed' for r in events):return
    start=time.monotonic();events.append(dict(stage=name,status='running',time=datetime.datetime.now(datetime.timezone.utc).isoformat()));write_json(EVENTS,events)
    print('Starting '+name,flush=True)
    with (RESULTS/f'addendum2-{name}-console.txt').open('a') as out:
        result=subprocess.run([sys.executable,str(EXP/'tools'/script),*args],stdout=out,stderr=subprocess.STDOUT)
    events.append(dict(stage=name,status='completed' if result.returncode==0 else 'failed',seconds=round(time.monotonic()-start,3),exit_code=result.returncode,time=datetime.datetime.now(datetime.timezone.utc).isoformat()));write_json(EVENTS,events)
    print(json.dumps(events[-1]),flush=True)
    assert result.returncode==0,name+' failed; inspect its archived console before resuming'

def empty_records():
    # The original Ouédraogo suite is empty, so its zero coverage is an exact
    # structural reference. No Java test, JaCoCo, or measurement is run here.
    assert not denominator(GROUP) and not denominator('ouedraogo')
    for folder in ['raw','raw-coverage']:
        for p in sorted((RESULTS/folder).glob('*-ouedraogo.json')):
            dest=p.with_name(p.name.replace('-ouedraogo.json','-ouedraogo-adj.json'))
            if dest.exists():continue
            data=json.loads(p.read_text());assert not data['methods'] and not data['files']
            data['group']=GROUP;data['seconds']=0;data['derivation']=dict(reason='empty retained suite; measurement skipped per addendum',source=str(p.relative_to(EXP)),source_sha256=hashlib.sha256(p.read_bytes()).hexdigest())
            if 'coverage_report' in data:
                data['coverage_report']={**data['coverage_report'],'seconds':0,'derived_not_executed':True}
            write_json(dest,data)
    for p in sorted((RESULTS/'coverage').glob('*-ouedraogo*')):
        if '-ouedraogo-adj' in p.name:continue
        dest=p.with_name(p.name.replace('-ouedraogo','-ouedraogo-adj'))
        if not dest.exists():shutil.copyfile(p,dest)
    write_json(RESULTS/'addendum2-empty-suite.json',dict(measurements_skipped=True,reason='zero retained methods',raw_records=229,coverage_records=181,coverage_derivation='Copied original empty Ouédraogo executable universes with zero hits; no new execution'))


def main():
    while not (RESULTS/'ouedraogo-adj-round1.json').exists():time.sleep(5)
    generation=json.loads((RESULTS/'ouedraogo-adj-round1.json').read_text());assert generation['model_calls']==25
    if not (BASE/'import-fixes.json').exists():stage('imports','repair_adjusted_imports.py')
    assert len(json.loads((BASE/'import-fixes.json').read_text())['files'])==len(list((BASE/'sources').rglob('*.java'))),'Partial import repair: inspect preserved per-file evidence before resuming'
    if not (BASE/'funnel.json').exists():stage('screen','validate_round.py','--group',GROUP,'--round','round1')
    stage('diagnostics','describe_round.py',GROUP,'round1')
    stage('control','report_adjusted.py','control')
    if denominator(GROUP):
        stage('phase-a','run_phase.py','--phase','a','--workers','2','--groups',GROUP,'--result-suffix=-ouedraogo-adj')
        assert not json.loads((RESULTS/'phase-a-ouedraogo-adj.json').read_text())['failed']
    else:empty_records()
    stage('survival','report_adjusted.py','survival')
    if denominator(GROUP):
        stage('phase-b','run_phase.py','--phase','b','--workers','2','--groups',GROUP,'--result-suffix=-ouedraogo-adj')
        assert not json.loads((RESULTS/'phase-b-ouedraogo-adj.json').read_text())['failed']
    stage('coverage','report_adjusted.py','coverage')
    stage('verify','report_adjusted.py','verify')
    print('Adjusted measurements and reports complete; documentation and commit remain.',flush=True)

if __name__=='__main__':main()
