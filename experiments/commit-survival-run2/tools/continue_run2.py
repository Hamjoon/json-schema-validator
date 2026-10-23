"""Host orchestration; every build/test/model operation executes in Docker."""
import csv
import datetime
import json
from pathlib import Path
import subprocess
import time

EXP=Path(__file__).resolve().parents[1]
REPO=EXP.parents[1]
KTESTER=REPO.parent/'KTester'
RESULTS=EXP/'results'
TRAILER='Co-authored-by: Codex <noreply@openai.com>'
CONTAINER_TOOLS='/repo/experiments/commit-survival-run2/tools'


def save(path,data):
    temp=path.with_suffix(path.suffix+'.tmp');temp.write_text(json.dumps(data,indent=2)+'\n');temp.replace(path)


def event(stage,status,**extra):
    path=RESULTS/'driver-events.json';records=json.loads(path.read_text()) if path.exists() else []
    records.append(dict(time=datetime.datetime.now(datetime.timezone.utc).isoformat(),stage=stage,status=status,**extra));save(path,records)
    if not (RESULTS/'final-summary.json').exists():
        (RESULTS/'STATUS.md').write_text(f'# Run 2 status\n\n{stage}: {status}. See driver-events.json and per-stage logs. Scope: 5 classes / 64 focal methods; Dev: 185 methods.\n')
    print(json.dumps(records[-1]),flush=True)


def run(stage,script,*args):
    start=time.monotonic();event(stage,'running')
    command=['docker','compose','-f','compose.yml','-f','compose.commit-survival.yml','run','--rm','ktester','python',CONTAINER_TOOLS+'/'+script,*args]
    with (RESULTS/(stage+'-console.txt')).open('a') as output:
        result=subprocess.run(command,cwd=KTESTER/'docker',stdout=output,stderr=subprocess.STDOUT)
    event(stage,'completed' if result.returncode==0 else 'failed',exit_code=result.returncode,seconds=round(time.monotonic()-start,3))
    if result.returncode:raise RuntimeError(f'{stage} exited {result.returncode}; inspect preserved log before retrying')


def checkpoint(step,title,details):
    (EXP/f'docs/handover-part{step}.md').write_text('# Step '+str(step)+' handover\n\n'+details+'\n\nSee results/driver-events.json for execution times and logs.\n')
    subprocess.run(['git','add',str(EXP.relative_to(REPO))],cwd=REPO,check=True)
    subprocess.run(['git','commit','-q','--only','-m',title,'-m',TRAILER,'--',str(EXP.relative_to(REPO))],cwd=REPO,check=True)
    event('commit-step'+str(step),'completed',commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=REPO,text=True).strip())


def count(group):
    return len(list(csv.DictReader((RESULTS/f'llm-{group}-denominator.csv').open())))


def screen(group,label):
    run(f'{group}-{label}-screening','validate_round.py','--group',group,'--round',label)
    run(f'{group}-{label}-diagnostics','describe_round.py',group,label)
    return json.loads((EXP/'generated'/group/label/'funnel.json').read_text())


def main():
    event('await-existing-ktester-round1','running')
    while not (RESULTS/'ktester-round1.json').exists():time.sleep(5)
    funnel=screen('ktester','round1')
    if funnel['compiled']==0:
        event('ktester-zero-compiled-retry','waiting-60-seconds');time.sleep(60)
        run('ktester-round1-retry-generation','generate_ktester.py','--round','1','--attempt','2')
        funnel=screen('ktester','round1-retry')
    checkpoint(2,'Screen the single KTester round for run 2',json.dumps(funnel,indent=2))
    for round_number in range(1,4):
        label=f'round{round_number}'
        run('ouedraogo-'+label+'-generation','generate_ouedraogo.py','--round',str(round_number))
        funnel=screen('ouedraogo',label)
        if funnel['compiled']==0:
            event('ouedraogo-'+label+'-zero-compiled-retry','waiting-60-seconds');time.sleep(60)
            run('ouedraogo-'+label+'-retry-generation','generate_ouedraogo.py','--round',str(round_number),'--attempt','2')
            funnel=screen('ouedraogo',label+'-retry')
        if count('ouedraogo')>=185:break
    checkpoint(3,'Freeze the screened Ouedraogo denominator for run 2',json.dumps({'kept':count('ouedraogo'),'last_round':round_number,'last_funnel':funnel},indent=2))
    if count('ktester')+count('ouedraogo')==0:
        event('mandatory-stop','both-generated-groups-empty')
        (EXP/'docs/handover-final.md').write_text('# Mandatory stop\n\nBoth generated groups have zero retained methods. See funnels and driver-events.json. No Phase A/B measurements.\n')
        checkpoint(4,'Record empty-generation stop for run 2','Both generated groups are empty; no fabricated survival or coverage.')
        return
    run('anchor-control','control_at_t.py')
    checkpoint(4,'Verify all three run 2 denominators at the anchor',(RESULTS/'control-at-t.md').read_text())
    run('phase-a','run_phase.py','--phase','a','--workers','4')
    run('survival-aggregation','aggregate_survival.py')
    checkpoint(5,'Complete run 2 survival across the full commit range',(RESULTS/'survival-consistency.json').read_text())
    # coverage indices and changed-line inputs are copied, never recomputed.
    run('phase-b','run_phase.py','--phase','b','--workers','4')
    checkpoint(6,'Complete run 2 alive-entering coverage measurements',(RESULTS/'phase-b.json').read_text())
    run('coverage-aggregation','aggregate_coverage.py')
    run('final-verification','final_check.py')
    run('artifact-verification','audit_run2.py')
    run('final-report','close_run2.py')
    final_status=(RESULTS/'STATUS.md').read_text()
    checkpoint(7,'Report run 2 survival, coverage and final verification',(RESULTS/'final-summary.json').read_text())
    (RESULTS/'STATUS.md').write_text(final_status)
    subprocess.run(['git','add',str(EXP.relative_to(REPO))],cwd=REPO,check=True)
    subprocess.run(['git','commit','-q','--only','-m','Finalize run 2 execution ledger','-m',TRAILER,'--',str(EXP.relative_to(REPO))],cwd=REPO,check=True)
    subprocess.run(['docker','compose','-f','compose.yml','-f','compose.commit-survival.yml','run','--rm','ktester','python',CONTAINER_TOOLS+'/record_ktester_completion.py'],cwd=KTESTER/'docker',check=True)
    paths=['data/commit-survival/json-schema-validator/rounds/run2-round1','data/commit-survival/json-schema-validator/experiment-result-run2.json']
    if (KTESTER/'data/commit-survival/json-schema-validator/rounds/run2-round1-retry').exists():paths.append('data/commit-survival/json-schema-validator/rounds/run2-round1-retry')
    subprocess.run(['git','add',*paths],cwd=KTESTER,check=True)
    subprocess.run(['git','commit','-q','--only','-m','Archive run 2 generation and completed experiment record','-m',TRAILER,'--',*paths],cwd=KTESTER,check=True)
    # The final reporting command owns STATUS; do not replace it with a driver status.
    print('RUN2_COMPLETE',flush=True)


if __name__=='__main__':
    try:main()
    except Exception as error:
        event('driver','needs-recovery',error=str(error))
        raise
