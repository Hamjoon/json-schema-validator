"""Cross-check complete measurements and the exact frozen method selections."""
import collections
import csv
import hashlib
import json
from start import EXP, RESULTS, write_json
from run_commit import GROUPS, denominator
from aggregate_survival import survival_record_path


def main():
    rows={g:denominator(g) for g in GROUPS}
    frozen=json.loads((RESULTS/'frozen-denominators.json').read_text())
    assert rows==frozen,'Denominator changed after final anchor control'
    commits=list(csv.DictReader((RESULTS/'commits.csv').open()))
    entering=json.loads((RESULTS/'alive-entering.json').read_text())
    indices=[int(r['index']) for r in csv.DictReader((RESULTS/'coverage-commits.csv').open())]
    mismatches=[];raw_count=0;coverage_count=0;flaky=0
    for phase,folder,selected_indices in [('a','raw',[int(r['index']) for r in commits]),('b','raw-coverage',indices)]:
        for i in selected_indices:
            if i and json.loads((RESULTS/f'builds/{i}.json').read_text())['build']!='ok':continue
            for g in GROUPS:
                record=json.loads((survival_record_path(i,g) if phase=='a' else RESULTS/f'{folder}/{i}-{g}.json').read_text())
                expected=rows[g] if phase=='a' else [r for r in rows[g] if r['id'] in entering[str(i)][g]]
                assert record['status']=='evaluated',(phase,i,g)
                assert set(record['requested_ids'])=={r['id'] for r in expected},(phase,i,g)
                assert len(record['methods'])==len(expected),(phase,i,g)
                digest=hashlib.sha256(json.dumps([(r['id'],hashlib.sha256((EXP/r['file']).read_bytes()).hexdigest()) for r in expected],sort_keys=True).encode()).hexdigest()
                assert digest==record['input_sha256'],(phase,i,g,'source changed')
                if phase=='a':
                    raw_count+=1;flaky+=sum(r['flaky'] for r in record['methods'])
                else:
                    coverage_count+=1
                    assert record['coverage_report']['exit_code']==0
                    if i:
                        a=json.loads(survival_record_path(i,g).read_text())
                        states={r['id']:r['state'] for r in a['methods']}
                        mismatches.extend(dict(index=i,group=g,id=r['id'],phase_a=states[r['id']],phase_b=r['state']) for r in record['methods'] if states[r['id']]!=r['state'])
    survival=json.loads((RESULTS/'survival-consistency.json').read_text())
    coverage=json.loads((RESULTS/'coverage-consistency.json').read_text())
    assert survival['nonincreasing'] and survival['index_zero_equals_denominators'] and survival['raw_count_matches']
    assert not survival['missing_records'] and not coverage['missing']
    assert len(coverage['identical_executable_line_sets'])==len(indices)
    result=dict(survival_records=raw_count,measured_survival_records=survival['measured_raw_records'],derived_survival_records=survival['derived_raw_records'],coverage_records=coverage_count,frozen_denominators_unchanged=True,source_fingerprints_match=True,coverage_selections_match_alive_entering=True,phase_a_flaky_pass_records=flaky,instrumented_state_differences=mismatches,first_failure_kinds=dict(collections.Counter(r['kind'] for r in csv.DictReader((RESULTS/'first-failures.csv').open()))))
    write_json(RESULTS/'final-verification.json',result)
    print(json.dumps(result),flush=True)


if __name__=='__main__':main()
