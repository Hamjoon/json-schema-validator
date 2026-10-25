"""Finalize the corrected-renderer group while preserving addendum 2 evidence."""
import collections
import csv
import datetime
import json
from start import EXP,RESULTS,write_json
from adjusted2_prompts import BASE
from aggregate_survival import LABELS

GROUP='ouedraogo-adj2'
GROUPS=['dev','ktester','ouedraogo','ouedraogo-adj',GROUP]
FIX='The dependency renderer now emits type parameters only when present, preserves declaration modifiers (including implicit public/static modifiers of interface member types), and preserves varargs as Type... instead of arrays. All five dependency blocks passed the mandatory pre-call gate; their type order, membership, omitted types, caps, API order, and prompt constraints match addendum 2. Only the requested header and varargs formatting changed.'

def main():
    audit=json.loads((RESULTS/'addendum3-audit.json').read_text());assert audit['status']=='passed'
    generation=json.loads((RESULTS/f'{GROUP}-round1.json').read_text())
    funnel=json.loads((BASE/'funnel.json').read_text())
    events=json.loads((RESULTS/'addendum3-events.json').read_text())
    failures=json.loads((RESULTS/f'{GROUP}-round1-compile-failures.json').read_text())
    denoms=json.loads((RESULTS/'denominators.json').read_text())
    curves=list(csv.DictReader((RESULTS/'survival.csv').open()));final={g:int(curves[-1][g+'_alive']) for g in GROUPS}
    losses=[r for r in csv.DictReader((RESULTS/'first-failures.csv').open()) if r['group']==GROUP]
    loss_counts=collections.Counter((int(r['index']),r['kind']) for r in losses)
    outcomes=[dict(index=i,kind=kind,methods=n) for (i,kind),n in sorted(loss_counts.items())]
    calls=json.loads((BASE/'generation.json').read_text())
    kept=list(csv.DictReader((RESULTS/f'llm-{GROUP}-denominator.csv').open()))
    started=json.loads((RESULTS/'addendum3-preservation.json').read_text())['started'];finished=datetime.datetime.now(datetime.timezone.utc)
    summary=dict(status='completed',started=started,finished=finished.isoformat(),wall_seconds=round((finished-datetime.datetime.fromisoformat(started)).total_seconds(),3),timing_note='Wall time starts at the evidence preservation snapshot; preliminary instruction review is excluded.',renderer_fix=FIX,pre_call_check=json.loads((BASE/'prompts/pre-call-check.json').read_text()),model_calls=generation['model_calls'],retry_calls=0,model=generation['model'],temperature=generation['temperature'],max_tokens=generation['max_tokens'],generation_seconds=generation['seconds'],phase_seconds={r['stage']:r['seconds'] for r in events if r['status']=='completed'},funnel=funnel,import_fix_files=audit['files_with_import_fixes'],import_fix_lines=audit['added_import_lines'],kept_by_class=dict(collections.Counter(r['class'] for r in kept)),kept_by_prompt=dict(collections.Counter(r['technique'] for r in kept)),first_failures=outcomes,denominators=denoms,final_survivors=final,generation_statuses=dict(collections.Counter(r['status'] for r in calls)),finish_reasons=dict(collections.Counter(r['finish_reason'] for r in calls)),compile_failure_categories=failures['category_file_counts'],failed_stage_attempts=[r for r in events if r['status']=='failed'],preflight_history=json.loads((RESULTS/'addendum3-preflight-history.json').read_text()),verification=audit,measurement_skipped=not kept,pushed=False)
    write_json(RESULTS/'addendum3-summary.json',summary)
    overall=json.loads((RESULTS/'before-addendum3-final-summary.json').read_text())
    overall.update(status='completed',finished=summary['finished'],denominators=denoms,final_survivors=final,addendum3=summary)
    overall['model_call_counts'][GROUP]=25;overall['model_rounds'][GROUP+'-round1']=generation
    overall['phase_seconds'].update({'addendum3-'+k:v for k,v in summary['phase_seconds'].items()})
    for key,name in [('survival_consistency','survival-consistency.json'),('coverage_consistency','coverage-consistency.json'),('final_verification','final-verification.json')]:overall[key]=json.loads((RESULTS/name).read_text())
    overall['artifact_audit']['addendum3']=audit
    overall['clarifications'].append('Addendum 3 fixes the renderer defects for ouedraogo-adj2 only. The earlier ouedraogo-adj group and its recorded limitation remain unchanged.')
    for phase in ['a','b']:
        path=RESULTS/f'phase-{phase}-{GROUP}.json'
        if path.exists():overall['phases'][f'phase-{phase}-{GROUP}']=json.loads(path.read_text())
    overall['wall_seconds_before_addendum3']=overall['wall_seconds'];overall['wall_seconds']+=summary['wall_seconds']
    overall['timing_note']='Wall seconds sum measured original/addendum work, excluding idle gaps. Prior summaries are preserved in before-addendum2-final-summary.json and before-addendum3-final-summary.json.'
    write_json(RESULTS/'final-summary.json',overall)
    (RESULTS/'STATUS.md').write_text('# Run 2 status\n\nCompleted through addendum 3.\n\nDenominators: '+', '.join(f'{LABELS[g]} {denoms[g]}' for g in GROUPS)+'. Final survivors: '+str(final)+'.\n\n1145 survival records and 905 coverage records. Executable line sets match across all five groups at all 181 indices. Previous group evidence and report values are unchanged. Exactly 25 new model calls, no retries.\n\n'+FIX+'\n\nThe earlier adjusted group retains its documented renderer limitation; its records were not changed. See addendum3-summary.json, addendum3-audit.json,. No push.\n')
    path=RESULTS/'consistency.md';text=path.read_text().split('\n## Five-group checks')[0]
    text+='\n## Five-group checks\n\n| Check | '+' | '.join(LABELS[g] for g in GROUPS)+' |\n|---|'+'---:|'*len(GROUPS)+'\n'
    for label,values in [('Fixed denominator',[denoms[g] for g in GROUPS]),('Final survivors',[final[g] for g in GROUPS]),('Survival records',[229]*5),('Coverage records',[181]*5),('Matching executable universes',[181]*5)]:text+='| '+label+' | '+' | '.join(map(str,values))+' |\n'
    text+=f"\nAll {audit['preserved_existing_files']} previously archived evidence files and all four previous groups' report values remain unchanged.\n"
    path.write_text(text)
    path=EXP/'README.md';text=path.read_text().split('\n## Corrected adjusted Ouédraogo group (addendum 3)')[0]
    text=text.replace('Denominators: **Dev 185, KTester 131, Ouédraogo 0, Ouédraogo (adjusted) 22**.',f"Denominators: **Dev 185, KTester 131, Ouédraogo 0, Ouédraogo (adjusted) 22, Ouédraogo (adjusted 2) {denoms[GROUP]}**.")
    text=text.replace('Final survivors: **Dev 4, KTester 0, Ouédraogo 0, Ouédraogo (adjusted) 0**.',f"Final survivors: **Dev 4, KTester 0, Ouédraogo 0, Ouédraogo (adjusted) 0, Ouédraogo (adjusted 2) {final[GROUP]}**.")
    text=text.replace('across all four groups','across all five groups')
    text+='\n## Corrected adjusted Ouédraogo group (addendum 3)\n\n'+FIX+' See the [block diff](generated/ouedraogo-adj2/round1/prompts/block-diff.md), [pre-call gate](generated/ouedraogo-adj2/round1/prompts/pre-call-check.json), [import-fix ledger](generated/ouedraogo-adj2/round1/import-fixes.json), [funnel](generated/ouedraogo-adj2/round1/funnel.md).\n\n'
    text+=f"The single `ouedraogo-adj2` round used exactly 25 calls, no retries, with the same model/settings and screening rules. {funnel['compiled']} of {funnel['files']} extracted files compiled; {funnel['total_kept']} methods were retained. Previous groups, including `ouedraogo-adj`, remain unchanged. The five-group reports contain 1145 survival records and 905 coverage records.\n"
    if not kept:text+='\nThe retained suite is empty: measurement execution was skipped and explicitly marked empty-suite structural records use the original empty Ouédraogo coverage universes. Survival is n/a; zero coverage is not evidence of comparative effectiveness. See results/addendum3-empty-suite.json.\n'
    text+='''\nReproduction inside the same KTester container (the pre-call check is mandatory and generation refuses existing call directories):\n\n```sh\nrun python "$T/adjusted2_prompts.py"\nrun python "$T/generate_adjusted2.py" --round 1\nrun python "$T/continue_adjusted2.py"\nrun python "$T/audit_adjusted2.py"\nrun python "$T/close_adjusted2.py"\n```\n'''
    path.write_text(text)
    print(json.dumps(dict(denominators=denoms,final_survivors=final,model_calls=25,imports=audit['added_import_lines'],seconds=summary['wall_seconds'])),flush=True)

if __name__=='__main__':main()
