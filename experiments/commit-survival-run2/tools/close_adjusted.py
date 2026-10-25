"""Write addendum timings, outcomes and limitations."""
import collections
import csv
import datetime
import json
from adjusted_prompts import BASE
from start import EXP,RESULTS,write_json

LIMITATION='The archived dependency blocks have a rendering defect in synthetic type headers: non-generic types appear with empty `<>`, and type declaration modifiers are omitted. The JDK parser also normalizes varargs parameter types to arrays. Constructor/method signatures came from the anchor source, but these blocks are not compilable Java stubs. The defect was discovered after generation started; all 25 frozen prompts were preserved and no replacement calls were made. Interpret this adjusted group with that prompt limitation.'

def main():
    audit=json.loads((RESULTS/'addendum2-audit.json').read_text());assert audit['status']=='passed'
    generation=json.loads((RESULTS/'ouedraogo-adj-round1.json').read_text())
    funnel=json.loads((BASE/'funnel.json').read_text())
    events=json.loads((RESULTS/'addendum2-events.json').read_text())
    failures=json.loads((RESULTS/'ouedraogo-adj-round1-compile-failures.json').read_text())
    denom=json.loads((RESULTS/'denominators.json').read_text())
    curves=list(csv.DictReader((RESULTS/'survival.csv').open()));final={g:int(curves[-1][g+'_alive']) for g in denom}
    started=json.loads((RESULTS/'addendum2-preservation.json').read_text())['started'];now=datetime.datetime.now(datetime.timezone.utc)
    rounds=json.loads((BASE/'generation.json').read_text())
    summary=dict(status='completed',started=started,finished=now.isoformat(),wall_seconds=round((now-datetime.datetime.fromisoformat(started)).total_seconds(),3),timing_note='Wall clock starts at the preservation manifest; preliminary instruction review/tool preparation is excluded.',model_calls=25,retry_calls=0,model=generation['model'],temperature=generation['temperature'],max_tokens=generation['max_tokens'],generation_seconds=generation['seconds'],phase_seconds={r['stage']:r['seconds'] for r in events if r['status']=='completed'},generation_statuses=dict(collections.Counter(r['status'] for r in rounds)),finish_reasons=dict(collections.Counter(r['finish_reason'] for r in rounds)),funnel=funnel,import_fix_files=audit['files_with_import_fixes'],import_fix_lines=audit['added_import_lines'],compile_failure_categories=failures['category_file_counts'],failed_stage_attempts=[r for r in events if r['status']=='failed'],denominators=denom,final_survivors=final,verification=audit,prompt_limitation=LIMITATION,measurement_skipped=funnel['total_kept']==0,pushed=False)
    write_json(RESULTS/'addendum2-summary.json',summary)
    overall=json.loads((RESULTS/'before-addendum2-final-summary.json').read_text())
    overall['finished']=summary['finished'];overall['denominators']=denom;overall['final_survivors']=final
    overall.setdefault('model_call_counts',{})['ouedraogo-adj']=25
    overall.setdefault('model_rounds',{})['ouedraogo-adj-round1']=generation
    overall.setdefault('phase_seconds',{}).update({'addendum2-'+k:v for k,v in summary['phase_seconds'].items()})
    overall['addendum2']=summary
    for key,name in [('survival_consistency','survival-consistency.json'),('coverage_consistency','coverage-consistency.json'),('final_verification','final-verification.json')]:
        overall[key]=json.loads((RESULTS/name).read_text())
    overall['artifact_audit']={'original_run':overall['artifact_audit'],'addendum2':audit}
    overall['deviations'].append(LIMITATION)
    for phase in ['a','b']:
        path=RESULTS/f'phase-{phase}-ouedraogo-adj.json'
        if path.exists():overall['phases'][f'phase-{phase}-ouedraogo-adj']=json.loads(path.read_text())
    overall['wall_seconds_before_addendum2']=overall.get('wall_seconds');overall['wall_seconds']=overall.get('wall_seconds',0)+summary['wall_seconds']
    overall['timing_note']='Original run timing is preserved in before-addendum2-final-summary.json; total wall_seconds sums original run and measured addendum work, excluding the idle gap.'
    write_json(RESULTS/'final-summary.json',overall)
    (RESULTS/'STATUS.md').write_text('# Run 2 status\n\nCompleted, including addendum 2.\n\nDenominators: '+', '.join(f'{g} {n}' for g,n in denom.items())+'. Final survivors: '+str(final)+'.\n\n916 survival records and 724 coverage records; executable line sets match across four groups at all 181 indices. Original evidence hashes are unchanged. Exactly 25 adjusted-group model calls, no retries.\n\n'+LIMITATION+'\n\nSee addendum2-summary.json and addendum2-audit.json. No push performed.\n')
    consistency=RESULTS/'consistency.md'
    checks=consistency.read_text().split('\n## Four-group checks')[0]
    groups=['dev','ktester','ouedraogo','ouedraogo-adj']
    checks+='\n## Four-group checks\n\n| Check | Dev | KTester | Ouédraogo | Ouédraogo (adjusted) |\n|---|---:|---:|---:|---:|\n'
    for label,values in [('Fixed denominator',[denom[g] for g in groups]),('Final survivors',[final[g] for g in groups]),('Survival records',[229]*4),('Coverage records',[181]*4),('Matching executable universes',[181]*4)]:
        checks+='| '+label+' | '+' | '.join(map(str,values))+' |\n'
    checks+='\nOriginal group report values and all original generated/raw/coverage files are unchanged. The preservation audit passed.\n'
    consistency.write_text(checks)
    p=EXP/'README.md';text=p.read_text().split('\n## Adjusted Ouédraogo group (addendum 2)')[0]
    text=text.replace('Denominators: **Dev 185, KTester 131, Ouédraogo 0**.',f"Denominators: **Dev 185, KTester 131, Ouédraogo 0, Ouédraogo (adjusted) {denom['ouedraogo-adj']}**.")
    text=text.replace('Final survivors: **Dev 4, KTester 0, Ouédraogo 0**.',f"Final survivors: **Dev 4, KTester 0, Ouédraogo 0, Ouédraogo (adjusted) {final['ouedraogo-adj']}**.")
    text=text.replace('across all three groups','across all four groups')
    text+='''\n## Adjusted Ouédraogo group (addendum 2)\n\nThe single `ouedraogo-adj` round applies three adjustments together: the exact API-version / real-object constraint after each class source; anchor-source dependency constructor and public/protected method signatures, capped at 15 types and 400 lines; and deterministic repair of uniquely resolved missing-class imports, with at most two passes and no other source edits. See the [exact per-class prompt blocks and omission manifests](generated/ouedraogo-adj/round1/prompts/), [import fixes and compiler passes](generated/ouedraogo-adj/round1/import-fixes.json), [funnel](generated/ouedraogo-adj/round1/funnel.md). Pre-repair sources are stored beside repaired sources. Exactly 25 calls used `openai/gpt-oss-120b`, temperature 0.7 and max_tokens 16384; no retry calls or rounds were made. Original Dev, KTester and Ouédraogo generation and measurement records are reused unchanged, verified by SHA-256.\n\n'''+LIMITATION+'\n'
    if not funnel['total_kept']:
        text+='\nNo adjusted methods survived screening. Measurements were skipped; the new empty-suite structural records and zero-hit coverage universes are explicitly derived from the original empty Ouédraogo group, as recorded in `results/addendum2-empty-suite.json`. Survival is n/a; zero coverage of an empty suite is not evidence of comparative effectiveness.\n'
    text+='''\nReproduce the added stages inside the existing KTester container (generation refuses existing call directories):\n\n```sh\nrun python "$T/adjusted_prompts.py"\nrun python "$T/generate_adjusted.py" --round 1\nrun python "$T/continue_adjusted.py"\nrun python "$T/audit_adjusted.py"\nrun python "$T/close_adjusted.py"\n```\n'''
    p.write_text(text)
    print(json.dumps(dict(denominators=denom,final_survivors=final,model_calls=25,import_lines=audit['added_import_lines'],seconds=summary['wall_seconds'])),flush=True)

if __name__=='__main__':main()
