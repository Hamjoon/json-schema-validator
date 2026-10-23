"""Verify immutable inputs and copied tool provenance without exposing secrets."""
import hashlib
import json
from pathlib import Path
import subprocess
from start import EXP, REPO, RESULTS, write_json


def main():
    manifest=json.loads((EXP/'dev/frozen-source-sha256.json').read_text())
    frozen=EXP/'dev/src-test-t'
    actual={str(p.relative_to(frozen)):hashlib.sha256(p.read_bytes()).hexdigest() for p in frozen.rglob('*') if p.is_file()}
    assert actual==manifest,'Frozen developer sources or fixtures changed'
    anchor=json.loads((RESULTS/'anchor.json').read_text())
    def unchanged(repo,base,paths):
        result=subprocess.run(['git','diff','--name-only',base,'--',*paths],cwd=repo,capture_output=True,text=True,check=True)
        assert not result.stdout.strip(),result.stdout
    unchanged(REPO,anchor['sha'],['src','pom.xml'])
    unchanged('/work','e37e51c82a5b7f3d9715d52ada5bf47bff56586e',['code','data/projects','data/project_index'])
    originals=[];adapted=[]
    for record in json.loads((EXP/'tools/origin.json').read_text()):
        path=EXP/'tools'/record['destination']
        digest=hashlib.sha256(path.read_bytes()).hexdigest()
        if record['destination'] in ['ouedraogo/generate.py','renumber_dataset_ids.py']:
            adapted.append(dict(file=record['destination'],current_sha256=digest,original_sha256=record['sha256']))
        else:
            assert digest==record['sha256'],record['destination']
            originals.append(record['destination'])
    links=[str(p.relative_to(EXP)) for root in ['dev','population','generated','tools'] for p in (EXP/root).rglob('*') if p.is_symlink()]
    assert not links,links
    result=dict(frozen_files=len(actual),frozen_manifest_matches=True,production_and_original_tests_unchanged=True,ktester_author_code_and_original_datasets_unchanged=True,verified_original_copies=originals,documented_adaptations=adapted,symlinks=links)
    write_json(RESULTS/'artifact-audit.json',result)
    print(json.dumps({k:v for k,v in result.items() if k not in ['verified_original_copies','documented_adaptations']}),flush=True)


if __name__=='__main__':main()
