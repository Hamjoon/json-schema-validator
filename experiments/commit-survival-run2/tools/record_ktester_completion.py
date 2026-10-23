"""Record the completed fork result in the isolated KTester dataset."""
import hashlib
import json
from pathlib import Path
from start import EXP, RESULTS, git, write_json


def main():
    path=RESULTS/'final-summary.json'
    summary=json.loads(path.read_text())
    assert summary['status']=='completed'
    record=dict(status=summary['status'],fork_commit=git('rev-parse','HEAD'),fork_branch=git('branch','--show-current'),anchor=json.loads((RESULTS/'anchor.json').read_text()),denominators=summary['denominators'],model_rounds=summary['model_rounds'],phase_seconds=summary['phase_seconds'],verification=summary['final_verification'],final_summary_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),report='/repo/experiments/commit-survival-run2/README.md',author_code_modified=False,pushed=False)
    target=Path('/work/data/commit-survival/json-schema-validator/experiment-result-run2.json')
    write_json(target,record)
    print(json.dumps(dict(status=record['status'],fork_commit=record['fork_commit'],denominators=record['denominators'])),flush=True)


if __name__=='__main__':main()
