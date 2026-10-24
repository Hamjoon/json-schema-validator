"""Audit append-only evidence, exactly 25 requests, import-only edits and five-group totals."""
import datetime
import collections
import csv
import hashlib
import json
import io
import subprocess
import re
from adjusted2_prompts import BASE,CONSTRAINT,TECHNIQUES
from start import EXP,REPO,RESULTS,write_json


def main():
    preserved=json.loads((RESULTS/'addendum3-preservation.json').read_text())['files']
    changed=[name for name,digest in preserved.items() if not (EXP/name).exists() or hashlib.sha256((EXP/name).read_bytes()).hexdigest()!=digest]
    assert not changed,changed
    from check_adjusted2_blocks import check
    check(write=False)
    requests=sorted((BASE/'runs').glob('*/*/request.json'));assert len(requests)==25
    assert not list((BASE/'runs').rglob('attempt-*'))
    gate=json.loads((BASE/'prompts/pre-call-check.json').read_text())
    for request in requests:
        record=json.loads(request.with_name('run.json').read_text())
        assert record['attempt']==1
        assert datetime.datetime.fromisoformat(record['timestamp'])>datetime.datetime.fromisoformat(gate['checked_at'])
    manifests=json.loads((BASE/'prompts/manifest.json').read_text());assert len(manifests)==25
    for r in manifests:
        name=r['class_name'];tech=r['technique']
        prompt=(BASE/'prompts/rendered'/name/(tech+'.txt')).read_text()
        assert hashlib.sha256(prompt.encode()).hexdigest()==r['sha256']
        assert prompt.count(CONSTRAINT)==1
        original=(EXP/'generated/ouedraogo/round1/prompts/rendered'/name/(tech+'.txt')).read_text()
        appended=(BASE/'prompts'/name/'appended-block.txt').read_text()
        assert prompt.replace(appended,'',1)==original
        req=json.loads((BASE/'runs'/name/tech/'request.json').read_text())
        assert req['messages']==[dict(role='user',content=prompt)]
        assert (req['model'],req['temperature'],req['max_tokens'])==('openai/gpt-oss-120b',0.7,16384)
    dependencies={}
    for p in (BASE/'prompts').glob('*/dependency-manifest.json'):
        r=json.loads(p.read_text());block=p.with_name('dependency-signatures.txt').read_text()
        assert len(r['included'])<=15 and len(block.splitlines())<=400
        assert r['ordered_types']==r['included']+r['omitted']
        dependencies[p.parent.name]=dict(types=len(r['included']),lines=len(block.splitlines()),omitted=r['omitted'])
    fixes=json.loads((BASE/'import-fixes.json').read_text())
    assert {r['file'] for r in fixes['files']}=={str(p.relative_to(EXP)) for p in (BASE/'sources').rglob('*.java')}
    imports=collections.Counter();files_fixed=0
    for r in fixes['files']:
        text=(EXP/r['pre_repair']).read_text();assert len(r['passes'])<=3
        for step in r['passes']:
            if not step['added_imports']:continue
            package=re.search(r'^\s*package\s+[^;]+;',text,re.M);position=package.end() if package else 0
            text=text[:position]+'\n'+''.join('import '+fqn+';\n' for fqn in step['added_imports'])+text[position:]
        assert text==(EXP/r['file']).read_text(),r['file']
        imports.update(r['added_imports']);files_fixed+=bool(r['added_imports'])
    funnel=json.loads((BASE/'funnel.json').read_text())
    denominator=list(csv.DictReader((RESULTS/'llm-ouedraogo-adj2-denominator.csv').open()))
    assert len(denominator)==funnel['total_kept']
    assert len({r['body_hash'] for r in denominator})==len(denominator)
    survival=json.loads((RESULTS/'survival-consistency.json').read_text())
    coverage=json.loads((RESULTS/'coverage-consistency.json').read_text())
    assert survival['raw_records']==survival['expected_raw_records']==1145
    assert len(coverage['identical_executable_line_sets'])==181
    assert not coverage['missing']
    assert len(list((RESULTS/'raw-coverage').glob('*.json')))==905
    assert len(list((RESULTS/'coverage').glob('*-lines.csv')))==905
    # Original per-method survival and entering selections must retain their values.
    for g in ['dev','ktester','ouedraogo','ouedraogo-adj']:
        assert json.loads((RESULTS/'denominators.json').read_text())[g]=={'dev':185,'ktester':131,'ouedraogo':0,'ouedraogo-adj':22}[g]
    base_commit=json.loads((RESULTS/'addendum3-preservation.json').read_text())['base_commit']
    def before(name):
        return subprocess.check_output(['git','-c','safe.directory='+str(REPO),'-C',str(REPO),'show',base_commit+':'+str((RESULTS/name).relative_to(REPO))],text=True)
    for name in ['survival.csv','changed-lines.csv']:
        old=list(csv.DictReader(io.StringIO(before(name))));new=list(csv.DictReader((RESULTS/name).open()))
        assert len(old)==len(new),name
        for a,b in zip(old,new):assert a=={k:b[k] for k in a},(name,a['index'])
    old=list(csv.DictReader(io.StringIO(before('first-failures.csv'))))
    new=[r for r in csv.DictReader((RESULTS/'first-failures.csv').open()) if r['group']!='ouedraogo-adj2']
    assert old==new
    for name in ['alive-entering.json','frozen-denominators.json']:
        old=json.loads(before(name));new=json.loads((RESULTS/name).read_text())
        if name=='alive-entering.json':
            for i,groups in old.items():assert groups=={g:new[i][g] for g in groups},i
        else:assert old=={g:new[g] for g in old}
    result=dict(status='passed',pre_call_gate_precedes_all_calls=True,original_group_report_values_unchanged=True,preserved_existing_files=len(preserved),modified_existing_evidence=changed,model_calls=25,retries=0,prompt_integrity=True,dependency_caps=dependencies,import_only_replay_matches=True,files_with_import_fixes=files_fixed,added_import_lines=sum(imports.values()),imports=dict(imports),funnel=funnel,raw_records=1145,coverage_records=905,matching_executable_universes=181)
    write_json(RESULTS/'addendum3-audit.json',result)
    print(json.dumps({k:v for k,v in result.items() if k not in ['dependency_caps','imports','funnel']}),flush=True)

if __name__=='__main__':main()
