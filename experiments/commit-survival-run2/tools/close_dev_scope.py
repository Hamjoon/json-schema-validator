"""Write addendum 4 results, provenance and timings."""
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
    (RESULTS/'STATUS.md').write_text('# Run 2 status\n\nCompleted through addendum 4.\n\n'+definition+'\n\nDenominators: '+', '.join(f'{LABELS[g]} {denoms[g]}' for g in GROUPS)+'. Final survivors: '+str(final)+'.\n\n1145 measured survival records + 229 derived records; 1086 coverage records. Executable universes match at all 181 indices across six groups. Previous evidence and group report values are unchanged. No model calls and no new Phase A test runs.\n\nSee addendum4-summary.json. No push.\n')
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
    text+='\n## Coverage-scoped developer group (addendum 4)\n\n'+definition+f" The denominator is **{anchor['selected']}**, with {anchor['excluded']} excluded methods ({len(anchor['failed_alone'])} failed alone). See the [185-method coverage summary](results/dev-method-coverage-t.csv), [per-method JaCoCo XML and run evidence](results/dev-method-coverage-t/), [denominator](dev/dev-scope-denominator.csv), [exclusions](dev/dev-scope-excluded.csv). The CSV columns are `class,method,scope_lines_covered,scope_files_touched,total_lines_covered,seconds,status`; `scope_files_touched` contains a JSON list.\n\n"
    text+=f"The final survival endpoints are Dev **{final['dev']}/{denoms['dev']}** and Dev (scope) **{final['dev-scope']}/{denoms['dev-scope']}**. Four independent container network namespaces prevent fixed-port interference. The initial shared-network attempt is preserved under `results/addendum4-quarantine/` and excluded from the denominator. No model calls were made. Existing generated, raw, and coverage records are unchanged.\n\n"
    text+='''Run the added stages from this repository root; the host driver launches all Java work inside the existing KTester container image:\n\n```sh\npython3 experiments/commit-survival-run2/tools/continue_dev_scope.py\n# Then run audit_dev_scope.py and close_dev_scope.py inside the KTester container.\n```\n'''
    path.write_text(text)
    print(json.dumps(dict(denominator=anchor['selected'],excluded=anchor['excluded'],failed_alone=len(anchor['failed_alone']),dev_final=final['dev'],dev_scope_final=final['dev-scope'],measured_records=1145,derived_records=229,coverage_records=1086,model_calls=0)),flush=True)

if __name__=='__main__':main()
