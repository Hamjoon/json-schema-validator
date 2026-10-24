"""Filter existing measured Dev evidence, preserving every selected outcome verbatim."""
import copy
import csv
import hashlib
import json
import time
from start import EXP,RESULTS,write_json
from run_dev_scope import denominator_for_group


def main():
    start=time.monotonic();rows=denominator_for_group('dev-scope');ids={r['id'] for r in rows}
    fingerprint=hashlib.sha256(json.dumps([(r['id'],hashlib.sha256((EXP/r['file']).read_bytes()).hexdigest()) for r in rows],sort_keys=True).encode()).hexdigest()
    frozen=json.loads((RESULTS/'frozen-denominators.json').read_text())
    if 'dev-scope' in frozen:assert frozen['dev-scope']==rows
    frozen={g:rows if g=='dev-scope' else frozen[g] for g in ['dev','dev-scope','ktester','ouedraogo','ouedraogo-adj','ouedraogo-adj2']}
    write_json(RESULTS/'frozen-denominators.json',frozen)
    count=0
    for commit in csv.DictReader((RESULTS/'commits.csv').open()):
        index=int(commit['index']);source=RESULTS/f'raw/{index}-dev.json'
        raw=json.loads(source.read_text());target=RESULTS/f'raw-derived/{index}-dev-scope.json'
        result=copy.deepcopy(raw)
        result.update(group='dev-scope',derived_from='dev',derived_from_record=str(source.relative_to(EXP)),derived_from_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),source_seconds=raw['seconds'],seconds=0,input_sha256=fingerprint,requested_ids=[r['id'] for r in rows],methods=[r for r in raw['methods'] if r['id'] in ids])
        result['files']=[]
        for file in raw['files']:
            methods=[m for m in file['methods'] if m['id'] in ids]
            if methods:result['files'].append({**file,'methods':methods,'derived_from':'dev'})
        assert {r['id'] for r in result['methods']}==ids
        assert {r['id'] for f in result['files'] for r in f['methods']}==ids
        if target.exists():assert json.loads(target.read_text())==result
        else:write_json(target,result)
        count+=1
    summary=dict(derived_records=count,denominator=len(rows),model_calls=0,new_phase_a_test_runs=0,seconds=round(time.monotonic()-start,3),provenance='Every selected method outcome, retry and flaky flag is copied verbatim from measured Dev; source run commands/logs are retained as evidence, not new executions.')
    write_json(RESULTS/'dev-scope-derivation.json',summary)
    print(json.dumps(summary),flush=True)

if __name__=='__main__':main()
