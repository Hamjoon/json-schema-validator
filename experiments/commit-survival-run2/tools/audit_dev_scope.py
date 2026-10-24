"""Audit isolated selection, exact derived outcomes, coverage and preserved evidence."""
import argparse
import collections
import csv
import hashlib
import io
import json
import subprocess
from start import EXP,REPO,RESULTS,write_json
from run_commit import denominator,line_rows
from run_dev_scope import denominator_for_group


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--anchor-only',action='store_true');args=parser.parse_args()
    preservation=json.loads((RESULTS/'addendum4-preservation.json').read_text())
    changed=[name for name,digest in preservation['files'].items() if not (EXP/name).exists() or hashlib.sha256((EXP/name).read_bytes()).hexdigest()!=digest]
    assert not changed,changed
    generated={str(p.relative_to(EXP)) for p in (EXP/'generated').rglob('*') if p.is_file()}
    assert generated=={name for name in preservation['files'] if name.startswith('generated/')},'Unexpected generation artifact'
    dev=denominator('dev');assert len(dev)==185
    scope={r['file'] for r in json.loads((EXP/'population/scope.json').read_text())['classes']}
    expected_universe={(r['file'],int(r['line'])) for r in csv.DictReader((RESULTS/'coverage/0-dev-lines.csv').open())}
    summary=list(csv.DictReader((RESULTS/'dev-method-coverage-t.csv').open()));assert len(summary)==185
    by_key={(r['class'],r['method']):r for r in summary};assert len(by_key)==185
    selected=[];failed=[];distribution=collections.Counter();isolated_scope_lines=set()
    for row in dev:
        label=row['class']+'#'+row['method'];path=RESULTS/'dev-method-coverage-t'/label
        record=json.loads(path.with_name(path.name+'.json').read_text())
        xml=RESULTS/'dev-method-coverage-t'/(label+'.xml')
        assert record['method']==row
        assert record['source_sha256']==hashlib.sha256((EXP/row['file']).read_bytes()).hexdigest()
        assert len(record['run']['methods'])==1
        method=record['run']['methods'][0];assert method['id']==row['id']
        assert len(method['baseline_states'])==1 and 'retry_state' not in method
        assert len(record['run']['runs'])==1
        command=record['run']['runs'][0]['command']
        assert command.count('--select-method')==1
        selector=command[command.index('--select-method')+1]
        assert selector.startswith(label+'('),(label,selector)
        assert '-Xmx1g' in command and '-XX:+UseSerialGC' in command
        assert len([s for s in command if s.startswith('-javaagent:')])==1
        lines=line_rows(xml);assert {(r['file'],r['line']) for r in lines}==expected_universe,label
        covered=[r for r in lines if r['covered']>0];scope_lines=[r for r in covered if r['file'] in scope]
        touched=sorted({r['file'] for r in scope_lines})
        assert record['scope_lines_covered']==len(scope_lines)
        assert record['scope_files_touched']==touched and record['total_lines_covered']==len(covered)
        csvrow=by_key[row['class'],row['method']]
        assert int(csvrow['scope_lines_covered'])==len(scope_lines)
        assert json.loads(csvrow['scope_files_touched'])==touched
        assert int(csvrow['total_lines_covered'])==len(covered) and csvrow['status']==record['status']
        if record['test_state']!='pass':failed.append(dict(id=row['id'],status=record['test_state'],message=record['message']))
        if record['status']=='pass' and scope_lines:
            selected.append(row);distribution.update(touched)
            isolated_scope_lines.update((r['file'],r['line']) for r in scope_lines)
    assert selected==denominator_for_group('dev-scope')
    ids={r['id'] for r in selected};keys={(r['class'],r['method']) for r in selected}
    excluded=list(csv.DictReader((EXP/'dev/dev-scope-excluded.csv').open()))
    assert {(r['class'],r['method']) for r in excluded}=={(r['class'],r['method']) for r in dev}-keys
    selection_summary=json.loads((RESULTS/'dev-method-coverage-t-summary.json').read_text())
    assert selection_summary['selected']==len(selected) and selection_summary['excluded']==len(excluded)
    assert selection_summary['per_scope_file']=={f:distribution[f] for f in sorted(scope)}
    if args.anchor_only:
        result=dict(status='passed',method_reports=185,denominator=len(selected),excluded=len(excluded),failed_alone=failed,per_scope_file=dict(distribution),isolated_selectors_verified=True,selection_recomputed_from_xml=True)
        write_json(RESULTS/'dev-scope-selection-audit.json',result)
        print(json.dumps(result),flush=True)
        return
    entering=json.loads((RESULTS/'alive-entering.json').read_text())
    assert all(set(groups['dev-scope'])==set(groups['dev'])&ids for groups in entering.values())
    for index in range(1,230):
        source=RESULTS/f'raw/{index}-dev.json';original=json.loads(source.read_text())
        derived=json.loads((RESULTS/f'raw-derived/{index}-dev-scope.json').read_text())
        assert derived['derived_from']=='dev' and derived['derived_from_sha256']==hashlib.sha256(source.read_bytes()).hexdigest()
        assert derived['methods']==[r for r in original['methods'] if r['id'] in ids],index
        assert set(derived['requested_ids'])==ids
        for file in derived['files']:
            old=next(f for f in original['files'] if f['file']==file['file'])
            assert file['methods']==[m for m in old['methods'] if m['id'] in ids]
            assert file['compile']==old['compile'] and file['runs']==old['runs']
        assert derived['seconds']==0
    assert len(list((RESULTS/'raw').glob('*.json')))==1145
    assert not list((RESULTS/'raw').glob('*-dev-scope.json'))
    assert len(list((RESULTS/'raw-derived').glob('*-dev-scope.json')))==229
    assert len(list((RESULTS/'raw-coverage').glob('*.json')))==1086
    assert len(list((RESULTS/'coverage').glob('*-lines.csv')))==1086
    runner_names=['run_commit.py','run_dev_scope.py','start.py','upstream/MethodSelectors.java','classpath-artifacts.json']
    runner_hashes={name:hashlib.sha256((EXP/'tools'/name).read_bytes()).hexdigest() for name in runner_names}
    subgroup_records=list((RESULTS/'raw-coverage').glob('*-dev-scope.json'));assert len(subgroup_records)==181
    for path in subgroup_records:
        assert json.loads(path.read_text())['runner_sha256']==runner_hashes,path.name
    coverage=json.loads((RESULTS/'coverage-consistency.json').read_text())
    assert not coverage['missing'] and len(coverage['identical_executable_line_sets'])==181
    assert not json.loads((RESULTS/'coverage-universe-mismatches.json').read_text())
    def anchor_scope_lines(group):
        return {(r['file'],int(r['line'])) for r in csv.DictReader((RESULTS/f'coverage/0-{group}-lines.csv').open()) if r['file'] in scope and int(r['covered'])>0}
    dev_lines=anchor_scope_lines('dev');subgroup_lines=anchor_scope_lines('dev-scope')
    anchor_scope_coverage=dict(executable_lines=sum(f in scope for f,line in expected_universe),dev_covered=len(dev_lines),dev_scope_covered=len(subgroup_lines),passing_isolated_union=len(isolated_scope_lines),subgroup_equals_passing_isolated_union=subgroup_lines==isolated_scope_lines,dev_only_lines=[dict(file=f,line=line) for f,line in sorted(dev_lines-subgroup_lines)])
    def before(name):
        return subprocess.check_output(['git','-c','safe.directory='+str(REPO),'-C',str(REPO),'show',preservation['base_commit']+':'+str((RESULTS/name).relative_to(REPO))],text=True)
    for name in ['survival.csv','changed-lines.csv']:
        old=list(csv.DictReader(io.StringIO(before(name))));new=list(csv.DictReader((RESULTS/name).open()))
        assert len(old)==len(new)
        for a,b in zip(old,new):assert a=={k:b[k] for k in a},(name,a['index'])
    old=list(csv.DictReader(io.StringIO(before('first-failures.csv'))))
    new=[r for r in csv.DictReader((RESULTS/'first-failures.csv').open()) if r['group']!='dev-scope'];assert old==new
    for name in ['alive-entering.json','frozen-denominators.json']:
        old=json.loads(before(name));new=json.loads((RESULTS/name).read_text())
        if name=='alive-entering.json':
            for i,groups in old.items():assert groups=={g:new[i][g] for g in groups}
        else:assert old=={g:new[g] for g in old}
    result=dict(status='passed',preserved_existing_files=len(preservation['files']),modified_existing_evidence=changed,previous_group_report_values_unchanged=True,model_calls=0,new_phase_a_test_runs=0,method_coverage_reports=185,isolated_workers=4,denominator=len(selected),excluded=len(excluded),failed_alone=failed,per_scope_file=dict(distribution),derived_outcomes_identical_to_dev=True,alive_entering_equals_dev_intersection=True,measured_survival_records=1145,derived_survival_records=229,coverage_records=1086,matching_executable_universes=181)
    result['anchor_scope_coverage']=anchor_scope_coverage
    result['coverage_runner_fingerprints_match']=True
    write_json(RESULTS/'addendum4-audit.json',result)
    print(json.dumps(result),flush=True)

if __name__=='__main__':main()
