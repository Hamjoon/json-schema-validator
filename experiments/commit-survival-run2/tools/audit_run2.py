"""Verify frozen inputs, reuse, selection and preserved model artifacts."""
import csv
import hashlib
import json
import os
from pathlib import Path
from start import EXP, RESULTS, SHARED, write_json
import audit_artifacts


def main():
    audit_artifacts.main()
    reused=json.loads((RESULTS/'reuse-manifest.json').read_text())
    adapted=[]
    for row in reused['files']:
        source=SHARED/row['path'];target=EXP/row['path']
        assert hashlib.sha256(source.read_bytes()).hexdigest()==row['sha256'],row['path']
        digest=hashlib.sha256(target.read_bytes()).hexdigest()
        if row['path'].startswith('tools/'):
            if digest!=row['sha256']:adapted.append(dict(file=row['path'],original_sha256=row['sha256'],current_sha256=digest))
        else:assert digest==row['sha256'],row['path']
    assert not (EXP/'checkouts').exists() and not (EXP/'work/classpaths').exists()
    dev=list(csv.DictReader((EXP/'dev/dev-denominator.csv').open()))
    expected=[{k:r[k] for k in ['class','method','kind']} for r in csv.DictReader((EXP/'dev/dev-methods-all.csv').open()) if r['stable']=='yes']
    assert dev==expected and len(dev)==185
    key=os.environ['OPENROUTER_API_KEY'].encode()
    scanned=0
    for root in [EXP/'generated',Path('/work/data/commit-survival/json-schema-validator/rounds/run2-round1')]:
        for p in root.rglob('*'):
            if p.is_file() and p.suffix not in ['.class','.jar']:
                assert key not in p.read_bytes(),'Credential found in artifact; do not commit '+str(p)
                scanned+=1
    sources=len(list((EXP/'dev/src-test-t/java').rglob('*.java')))
    whole=[]
    for p in (RESULTS/'raw').glob('*-dev.json'):
        record=json.loads(p.read_text());assert record['whole_frozen_suite_compile']['source_count']==sources
        whole.append(record['index'])
    assert len(whole)==229
    prompt_hashes={}
    for p in (EXP/'generated/ouedraogo').glob('*/prompts/rendered/*/*.txt'):
        identity='/'.join(p.parts[-2:]);digest=hashlib.sha256(p.read_bytes()).hexdigest()
        if identity in prompt_hashes:assert prompt_hashes[identity]==digest
        prompt_hashes[identity]=digest
    result=dict(reused_files_verified=len(reused['files']),adapted_tools=adapted,dev_denominator=185,
                frozen_java_source_count=sources,whole_suite_compile_indices=sorted(whole),
                original_caches_reused_in_place=True,credential_scan_files=scanned,credential_scan_passed=True,
                ouedraogo_prompts_identical_between_rounds=True,prompt_sha256=prompt_hashes)
    write_json(RESULTS/'run2-artifact-audit.json',result)
    print(json.dumps({k:v for k,v in result.items() if k not in ['adapted_tools','whole_suite_compile_indices','prompt_sha256']}),flush=True)


if __name__=='__main__':main()
