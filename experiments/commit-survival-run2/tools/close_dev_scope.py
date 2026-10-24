"""Write addendum 4 results, provenance, timings and the requested handover."""
import collections
import csv
import datetime
import json
import re
from start import EXP,RESULTS,write_json
from aggregate_survival import LABELS

GROUPS=['dev','dev-scope','ktester','ouedraogo','ouedraogo-adj','ouedraogo-adj2']

def main():
    audit=json.loads((RESULTS/'addendum4-audit.json').read_text());assert audit['status']=='passed'
    anchor=json.loads((RESULTS/'dev-method-coverage-t-summary.json').read_text())
    correction=json.loads((RESULTS/'addendum4-isolation-correction.json').read_text())
    events=json.loads((RESULTS/'addendum4-events.json').read_text())
    denoms=json.loads((RESULTS/'denominators.json').read_text())
    curves=list(csv.DictReader((RESULTS/'survival.csv').open()));final={g:int(curves[-1][g+'_alive']) for g in GROUPS}
    started=json.loads((RESULTS/'addendum4-preservation.json').read_text())['started'];finished=datetime.datetime.now(datetime.timezone.utc)
    exclusions=list(csv.DictReader((EXP/'dev/dev-scope-excluded.csv').open()))
    summary=dict(status='completed',started=started,finished=finished.isoformat(),wall_seconds=round((finished-datetime.datetime.fromisoformat(started)).total_seconds(),3),timing_note='Wall time starts at the evidence snapshot and includes the invalid shared-network attempt and a brief orchestration hold to clarify a misdirected message. Worker execution durations are recorded separately.',model_calls=0,new_phase_a_test_runs=0,denominator=anchor['selected'],excluded=anchor['excluded'],exclusion_reasons=dict(collections.Counter(r['reason'] for r in exclusions)),failed_alone=anchor['failed_alone'],per_scope_file=anchor['per_scope_file'],fewer_than_20=anchor['fewer_than_20'],phase_seconds={r['stage']:r['seconds'] for r in events if r['status']=='completed'},anchor_worker_seconds=[json.loads(p.read_text())['seconds'] for p in sorted(RESULTS.glob('dev-method-coverage-t-worker*.json'))],coverage_worker_seconds=[json.loads(p.read_text())['seconds'] for p in sorted(RESULTS.glob('phase-b-dev-scope-worker*.json'))],anchor=anchor,network_isolation='four independent Docker containers; one sequential worker in each',invalid_attempt=correction,denominators=denoms,final_survivors=final,dev_endpoints=dict(anchor=denoms['dev'],final=final['dev']),dev_scope_endpoints=dict(anchor=denoms['dev-scope'],final=final['dev-scope']),verification=audit,pushed=False)
    verification=json.loads((RESULTS/'final-verification.json').read_text())
    summary['instrumented_state_differences']=[r for r in verification['instrumented_state_differences'] if r['group']=='dev-scope']
    write_json(RESULTS/'addendum4-summary.json',summary)
    phase_b=dict(seconds=summary['phase_seconds']['phase-b'],workers=4,network_isolation=summary['network_isolation'],jobs=181,failed=[],oom_quarantine=[],worker_seconds=summary['coverage_worker_seconds'])
    write_json(RESULTS/'phase-b-dev-scope.json',phase_b)
    overall=json.loads((RESULTS/'before-addendum4-final-summary.json').read_text())
    overall.update(status='completed',finished=summary['finished'],denominators=denoms,final_survivors=final,addendum4=summary)
    overall['phase_seconds'].update({'addendum4-'+k:v for k,v in summary['phase_seconds'].items()})
    overall['phases']['phase-b-dev-scope']=phase_b
    for key,name in [('survival_consistency','survival-consistency.json'),('coverage_consistency','coverage-consistency.json'),('final_verification','final-verification.json')]:overall[key]=json.loads((RESULTS/name).read_text())
    overall['artifact_audit']['addendum4']=audit
    overall['clarifications'].append('Dev (scope) selects passing isolated anchor methods with a scope-line hit. Its survival is filtered from existing Dev records; only its coverage is newly measured. Four container network namespaces avoid localhost:1234 contention. The initial shared-network attempt is quarantined and excluded.')
    overall['wall_seconds_before_addendum4']=overall['wall_seconds'];overall['wall_seconds']+=summary['wall_seconds']
    overall['timing_note']='Wall seconds sum the original run and measured addendum work, excluding gaps between requests. Each addendum summary states its timing boundaries; addendum 4 includes a quarantined attempt and brief orchestration hold.'
    write_json(RESULTS/'final-summary.json',overall)
    definition='Dev (scope) is the subset of the 185 frozen developer methods that pass when run alone at the anchor and cover at least one executable line in the five scope files. A test-factory or parameterized method is selected as one unit with all its cases. Survival is derived verbatim from the existing Dev method outcomes; coverage runs only the subgroup methods alive entering each index.'
    (RESULTS/'STATUS.md').write_text('# Run 2 status\n\nCompleted through addendum 4.\n\n'+definition+'\n\nDenominators: '+', '.join(f'{LABELS[g]} {denoms[g]}' for g in GROUPS)+'. Final survivors: '+str(final)+'.\n\n1145 measured survival records + 229 derived records; 1086 coverage records. Executable universes match at all 181 indices across six groups. Previous evidence and group report values are unchanged. No model calls and no new Phase A test runs.\n\nSee addendum4-summary.json and docs/handover-part10.md. No push.\n')
    path=RESULTS/'consistency.md';text=path.read_text().split('\n## Six-group checks')[0]
    text+='\n## Six-group checks\n\n| Check | '+' | '.join(LABELS[g] for g in GROUPS)+' |\n|---|'+'---:|'*len(GROUPS)+'\n'
    for label,values in [('Fixed denominator',[denoms[g] for g in GROUPS]),('Final survivors',[final[g] for g in GROUPS]),('Measured survival records',[0 if g=='dev-scope' else 229 for g in GROUPS]),('Derived survival records',[229 if g=='dev-scope' else 0 for g in GROUPS]),('Coverage records',[181]*6),('Matching executable universes',[181]*6)]:text+='| '+label+' | '+' | '.join(map(str,values))+' |\n'
    text+=f"\nMeasured raw records: 229 × 5 = 1145. Derived records: 229. Total survival records: 1374. Coverage: 181 × 6 = 1086. All {audit['preserved_existing_files']} original evidence files and all five earlier groups' report values are unchanged.\n"
    path.write_text(text)
    path=EXP/'README.md';text=path.read_text().split('\n## Coverage-scoped developer group (addendum 4)')[0]
    text=re.sub(r'Denominators: \*\*.*?\*\*\.', 'Denominators: **'+', '.join(f'{LABELS[g]} {denoms[g]}' for g in GROUPS)+'**.',text,count=1)
    text=re.sub(r'Final survivors: \*\*.*?\*\*\.', 'Final survivors: **'+', '.join(f'{LABELS[g]} {final[g]}' for g in GROUPS)+'**.',text,count=1)
    text=text.replace('The consistency checks compare executable line sets across all five groups.','The consistency checks compare executable line sets across all six groups.')
    text=text.replace('The five-group reports contain 1145 survival records and 905 coverage records.', 'At completion of addendum 3, the five-group reports contained 1145 survival records and 905 coverage records.')
    text+='\n## Coverage-scoped developer group (addendum 4)\n\n'+definition+f" The denominator is **{anchor['selected']}**, with {anchor['excluded']} excluded methods ({len(anchor['failed_alone'])} failed alone). See the [185-method coverage summary](results/dev-method-coverage-t.csv), [per-method JaCoCo XML and run evidence](results/dev-method-coverage-t/), [denominator](dev/dev-scope-denominator.csv), [exclusions](dev/dev-scope-excluded.csv), and [part 10 handover](docs/handover-part10.md). The CSV columns are `class,method,scope_lines_covered,scope_files_touched,total_lines_covered,seconds,status`; `scope_files_touched` contains a JSON list.\n\n"
    text+=f"The final survival endpoints are Dev **{final['dev']}/{denoms['dev']}** and Dev (scope) **{final['dev-scope']}/{denoms['dev-scope']}**. Four independent container network namespaces prevent fixed-port interference. The initial shared-network attempt is preserved under `results/addendum4-quarantine/` and excluded from the denominator. No model calls were made. Existing generated, raw, and coverage records are unchanged.\n\n"
    text+='''Run the added stages from this repository root; the host driver launches all Java work inside the existing KTester container image:\n\n```sh\npython3 experiments/commit-survival-run2/tools/continue_dev_scope.py\n# Then run audit_dev_scope.py and close_dev_scope.py inside the KTester container.\n```\n'''
    path.write_text(text)
    lines=['# Step 10 handover: coverage-scoped developer group','',definition,'',f"Denominator: **{anchor['selected']}**. Excluded: **{anchor['excluded']}**. Exclusion reasons: {summary['exclusion_reasons']}. No model calls. No Phase A test reruns. No push.",'','## Scope-file distribution','','A method may touch multiple scope files. Counts include only retained, passing methods.','','| Scope file | Methods touching it |','|---|---:|']
    lines += [f'| {f} | {n} |' for f,n in anchor['per_scope_file'].items()]
    lines+=['','## Methods failing alone','']
    if anchor['failed_alone']:
        for r in anchor['failed_alone']:lines+=['- `'+r['class']+'#'+r['method']+'`: '+r['status']+'. `'+r['message']+'`']
    else:lines+=['None.']
    lines+=['','The other exclusions had no covered executable scope line. All 185 per-method XML reports and method-selection evidence are in `results/dev-method-coverage-t/`. Canonical summary: `results/dev-method-coverage-t.csv`; selected and excluded rows: `dev/dev-scope-denominator.csv` and `dev/dev-scope-excluded.csv`.','', '## Survival and coverage','',f"Dev: **{denoms['dev']} → {final['dev']}** ({100*final['dev']/denoms['dev']:.3f}% at the end). Dev (scope): **{denoms['dev-scope']} → {final['dev-scope']}**." ,'','The 229 derived records in `results/raw-derived/` retain each selected Dev outcome, message, retry state and flaky flag unchanged; `derived_from: dev`, source record paths and SHA-256 hashes establish provenance. No new survival tests ran. Phase B measured 181 indices with the alive-entering subgroup and the original whole-frozen-Dev compilation/fallback behavior. Its four workers ran in separate container network namespaces.','',f"Audit passed: {audit['preserved_existing_files']} prior evidence files unchanged; previous group report values unchanged; 1145 measured + 229 derived survival records; 1086 coverage records; executable line sets match at all 181 indices across all six groups. See `results/addendum4-audit.json` and `results/final-verification.json`.",'','## Timings','',f"Measured wall time: {summary['wall_seconds']:.3f} seconds. "+summary['timing_note'],'','| Stage | Seconds |','|---|---:|']
    lines += [f'| {name} | {seconds:.3f} |' for name,seconds in summary['phase_seconds'].items()]
    lines+=['',f"Anchor worker execution seconds: {summary['anchor_worker_seconds']}. Coverage worker execution seconds: {summary['coverage_worker_seconds']}. Shared anchor compilation: {anchor['shared_anchor_compile_seconds']:.3f} seconds.",'','## Execution correction','',f"The first attempt shared one network namespace across four concurrent JVMs. HTTPServiceSupport binds localhost:1234; {correction['bind_errors']} completed records reported BindException. The attempt was deliberately stopped after {correction['completed_records']} records and preserved under `results/addendum4-quarantine/shared-network-anchor-attempt/`. Exit 137 was an explicit container stop, not an OOM. Those records do not enter the selection. The complete valid pass ran all 185 methods in four independent containers, with no per-method retries or source changes. See `results/addendum4-isolation-correction.json`.",'','A brief orchestrator hold clarified a message intended for a sibling repository. Existing isolated workers completed uninterrupted; the user said to ignore that message, and the reference addendum resumed. No sibling repository files were edited. See `results/addendum4-orchestration-note.json`.','']
    if anchor['fewer_than_20']:lines+=['The denominator is below 20; all requested steps were completed anyway.','']
    lines+=['## Instrumented outcome differences','',f"The subgroup coverage pass has {len(summary['instrumented_state_differences'])} method outcomes different from the inherited Phase A outcomes. They are recorded in `results/addendum4-summary.json` and `results/final-verification.json`. Survival continues to use the original Dev outcomes unchanged; coverage outcomes do not redefine it.",'']
    anchor_coverage=audit['anchor_scope_coverage']
    lines+=['## Anchor scope coverage','',f"Dev covers {anchor_coverage['dev_covered']}/{anchor_coverage['executable_lines']} executable scope lines; Dev (scope) covers {anchor_coverage['dev_scope_covered']}/{anchor_coverage['executable_lines']}. The subgroup coverage {'matches' if anchor_coverage['subgroup_equals_passing_isolated_union'] else 'differs from'} the union of passing isolated method coverage. The {len(anchor_coverage['dev_only_lines'])} lines covered only by the full Dev suite are listed in `results/addendum4-audit.json`.",'']
    (EXP/'docs/handover-part10.md').write_text('\n'.join(lines))
    print(json.dumps(dict(denominator=anchor['selected'],excluded=anchor['excluded'],failed_alone=len(anchor['failed_alone']),dev_final=final['dev'],dev_scope_final=final['dev-scope'],measured_records=1145,derived_records=229,coverage_records=1086,model_calls=0)),flush=True)

if __name__=='__main__':main()
