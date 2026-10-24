"""Dev-scope Phase B: original Dev whole-suite compilation and selected class launches."""
import collections
import csv
import hashlib
import json
import shutil
import time
from start import EXP,REPO,RESULTS,SHARED,execute,write_json
from select_scope import csvout
from run_commit import denominator as original_denominator,classpath,run_file,line_rows


def denominator_for_group(group):
    if group!='dev-scope':return original_denominator(group)
    keys={(r['class'],r['method']) for r in csv.DictReader((EXP/'dev/dev-scope-denominator.csv').open())}
    rows=[r for r in original_denominator('dev') if (r['class'],r['method']) in keys]
    assert len(rows)==len(keys)
    return rows


def run_group(index,group,phase='a',selected_ids=None):
    assert group=='dev-scope' and phase=='b', 'Only the new subgroup coverage may run'
    started=time.monotonic();rows=denominator_for_group(group)
    if selected_ids is not None:rows=[r for r in rows if r['id'] in set(selected_ids)]
    output=RESULTS/('raw' if phase=='a' else 'raw-coverage' if phase=='b' else phase)/f'{index}-{group}.json'
    fingerprint=hashlib.sha256(json.dumps([(r['id'],hashlib.sha256((EXP/r['file']).read_bytes()).hexdigest()) for r in rows],sort_keys=True).encode()).hexdigest()
    runner_hashes={name:hashlib.sha256((EXP/'tools'/name).read_bytes()).hexdigest() for name in ['run_commit.py','run_dev_scope.py','start.py','upstream/MethodSelectors.java','classpath-artifacts.json']}
    if output.exists():
        old=json.loads(output.read_text());assert old['input_sha256']==fingerprint
        assert old.get('runner_sha256')==runner_hashes,'Runner changed since this measurement'
        return old
    work=EXP/f'work/phase-{phase}/{index}/{group}'
    if work.exists():shutil.rmtree(work)
    work.mkdir(parents=True)
    coverage=phase=='b';execfile=work/'jacoco.exec'
    by_file=collections.defaultdict(list)
    for r in rows:by_file[r['file']].append(r)
    files=[]
    suite_compile=None
    precompiled=None
    if group=='dev-scope':
        # Attempt every frozen Java source, including helpers and classes with no
        # denominator methods. On failure retain per-class failure attribution.
        folder,cp=classpath(index)
        suite_classes=work/'suite-classes';suite_classes.mkdir()
        sources=sorted((EXP/'dev/src-test-t/java').rglob('*.java'))
        suite_compile=execute(['javac','-J-Xmx512m','-cp',cp,'-sourcepath',str(EXP/'dev/src-test-t/java'),'-d',str(suite_classes),*[str(p) for p in sources]],folder,work/'suite-javac.txt',300)
        suite_compile['source_count']=len(sources)
        suite_compile['diagnostics']=(work/'suite-javac.txt').read_text(errors='replace')
        if suite_compile['exit_code']==0:precompiled=(suite_classes,suite_compile)
    for number,(filename,members) in enumerate(sorted(by_file.items())):
        files.append(run_file(index,'dev',filename,members,work/str(number),execfile if coverage else None,retry=phase in ['a','b'],precompiled=precompiled))
    commit=json.loads((RESULTS/'anchor.json').read_text()) if index==0 else next(r for r in csv.DictReader((RESULTS/'commits.csv').open()) if int(r['index'])==index)
    result=dict(index=index,sha=commit['sha'],group=group,phase=phase,status='evaluated',input_sha256=fingerprint,runner_sha256=runner_hashes,files=files,methods=[r for f in files for r in f['methods']],requested_ids=[r['id'] for r in rows])
    if suite_compile is not None:result['whole_frozen_suite_compile']=suite_compile
    if coverage:
        folder,_=classpath(index);xml=RESULTS/f'coverage/{index}-{group}.xml';xml.parent.mkdir(parents=True,exist_ok=True)
        run=execute(['java','-Xmx1g','-jar',str(SHARED/'work/libs/org.jacoco.cli-0.8.12-nodeps.jar'),'report',*([str(execfile)] if execfile.exists() else []),'--classfiles',str(folder/'target/classes'),'--sourcefiles',str(folder/'src/main/java'),'--xml',str(xml)],folder,work/'coverage-report.txt',300)
        result['coverage_report']=run
        if run['exit_code']==0:
            csvout(xml.with_name(xml.stem+'-lines.csv'),line_rows(xml),['file','line','missed','covered'])
        else:result['status']='coverage-fail'
    assert len(result['methods'])==len(rows)
    result['seconds']=round(time.monotonic()-started,3)
    write_json(output,result)
    return result


if __name__=='__main__':
    import argparse
    parser=argparse.ArgumentParser();parser.add_argument('--worker',type=int,required=True);args=parser.parse_args()
    assert 0<=args.worker<4
    started=time.monotonic()
    indices=[int(r['index']) for r in csv.DictReader((RESULTS/'coverage-commits.csv').open())]
    entering=json.loads((RESULTS/'alive-entering.json').read_text())
    completed=[]
    for number,index in enumerate(indices):
        if number%4!=args.worker:continue
        result=run_group(index,'dev-scope','b',entering[str(index)]['dev-scope'])
        assert result['status']=='evaluated',(index,result['status'])
        killed=[r for f in result['files'] for r in [f['compile'],*f['runs']] if r.get('exit_code') in [-9,137]]
        assert not killed,('Memory termination; quarantine new record before resuming',index)
        completed.append(index)
        print(json.dumps(dict(index=index,worker=args.worker,methods=len(result['methods']),states=dict(collections.Counter(m['state'] for m in result['methods'])),seconds=result['seconds'])),flush=True)
    write_json(RESULTS/f'phase-b-dev-scope-worker{args.worker}.json',dict(indices=completed,seconds=round(time.monotonic()-started,3),network_isolation='one container per sequential worker',failed=[]))
