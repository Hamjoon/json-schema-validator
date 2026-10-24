"""Run each frozen developer method alone at t; no retries or model calls."""
import argparse
import collections
import concurrent.futures
import csv
import hashlib
import json
import time
from start import EXP,REPO,RESULTS,SHARED,execute,write_json
from run_commit import denominator,classpath,run_file,line_rows
from select_scope import csvout

BASE=RESULTS/'dev-method-coverage-t'

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--prepare',action='store_true');parser.add_argument('--worker',type=int);parser.add_argument('--finalize',action='store_true');args=parser.parse_args()
    assert sum([args.prepare,args.worker is not None,args.finalize])==1
    started=time.monotonic();rows=denominator('dev');assert len(rows)==185
    scope={r['file'] for r in json.loads((EXP/'population/scope.json').read_text())['classes']}
    universe={(r['file'],int(r['line'])) for r in csv.DictReader((RESULTS/'coverage/0-dev-lines.csv').open())}
    folder,cp=classpath(0)
    classes=EXP/'work/dev-method-coverage-t/suite-classes';classes.mkdir(parents=True,exist_ok=True)
    sources=sorted((EXP/'dev/src-test-t/java').rglob('*.java'))
    if args.prepare:
        assert not (BASE/'anchor-compile.json').exists(),'Anchor compilation already prepared'
        prepared=time.time()
        compile_run=execute(['javac','-J-Xmx512m','-cp',cp,'-sourcepath',str(EXP/'dev/src-test-t/java'),'-d',str(classes),*[str(p) for p in sources]],folder,BASE/'logs/anchor-javac.txt',300)
        compile_run['prepared_unix']=prepared
        write_json(BASE/'anchor-compile.json',compile_run)
        assert compile_run['exit_code']==0,'Frozen anchor suite failed to compile'
        return
    compile_run=json.loads((BASE/'anchor-compile.json').read_text())
    assert compile_run['exit_code']==0
    def one(pair):
        number,row=pair;label=row['class']+'#'+row['method']
        output=BASE/(label+'.json')
        source_hash=hashlib.sha256((EXP/row['file']).read_bytes()).hexdigest()
        if output.exists():
            old=json.loads(output.read_text());assert old['method']['id']==row['id'] and old['source_sha256']==source_hash
            assert (BASE/(label+'.xml')).exists();return old
        begin=time.monotonic();part=BASE/'logs'/str(number)
        execfile=EXP/f'work/dev-method-coverage-t/{number}/jacoco.exec';execfile.parent.mkdir(parents=True,exist_ok=True)
        assert not execfile.exists(),'Partial per-method run exists; inspect before repeating'
        record=run_file(0,'dev',row['file'],[row],part,coverage_file=execfile,repetitions=1,retry=False,precompiled=(classes,compile_run))
        assert len(record['methods'])==1
        xml=BASE/(label+'.xml')
        report=execute(['java','-Xmx1g','-jar',str(SHARED/'work/libs/org.jacoco.cli-0.8.12-nodeps.jar'),'report',*([str(execfile)] if execfile.exists() else []),'--classfiles',str(folder/'target/classes'),'--sourcefiles',str(folder/'src/main/java'),'--xml',str(xml)],folder,part/'coverage-report.txt',300)
        method=record['methods'][0];status=method['state']
        lines=line_rows(xml) if report['exit_code']==0 else []
        if report['exit_code']!=0:status='coverage-fail'
        if report['exit_code']==0:assert {(r['file'],r['line']) for r in lines}==universe,label
        covered=[r for r in lines if r['covered']>0];in_scope=[r for r in covered if r['file'] in scope]
        result=dict(method=row,source_sha256=source_hash,run=record,coverage_report=report,coverage_xml=str(xml.relative_to(EXP)),scope_lines_covered=len(in_scope),scope_files_touched=sorted({r['file'] for r in in_scope}),total_lines_covered=len(covered),seconds=round(time.monotonic()-begin,3),status=status,test_state=method['state'],message=method.get('message',''),scope_lines=[dict(file=r['file'],line=r['line']) for r in in_scope])
        write_json(output,result)
        print(json.dumps(dict(number=number+1,method=label,status=status,scope_lines=len(in_scope))),flush=True)
        return result
    if args.worker is not None:
        assert 0<=args.worker<4
        completed=[one((number,row)) for number,row in enumerate(rows) if number%4==args.worker]
        write_json(RESULTS/f'dev-method-coverage-t-worker{args.worker}.json',dict(worker=args.worker,methods=len(completed),seconds=round(time.monotonic()-started,3),network_isolation='one Docker network namespace per sequential worker'))
        return
    records=[json.loads((BASE/(row['class']+'#'+row['method']+'.json')).read_text()) for row in rows]
    summary=[dict(**{'class':r['method']['class']},method=r['method']['method'],scope_lines_covered=r['scope_lines_covered'],scope_files_touched=json.dumps(r['scope_files_touched']),total_lines_covered=r['total_lines_covered'],seconds=r['seconds'],status=r['status']) for r in records]
    csvout(RESULTS/'dev-method-coverage-t.csv',summary,['class','method','scope_lines_covered','scope_files_touched','total_lines_covered','seconds','status'])
    selected=[r for r in records if r['status']=='pass' and r['scope_lines_covered']>0]
    excluded=[r for r in records if r not in selected]
    original=list(csv.DictReader((EXP/'dev/dev-denominator.csv').open()));keys={(r['method']['class'],r['method']['method']) for r in selected}
    csvout(EXP/'dev/dev-scope-denominator.csv',[r for r in original if (r['class'],r['method']) in keys],list(original[0]))
    excluded_rows=[dict(**r['method'],status=r['status'],scope_lines_covered=r['scope_lines_covered'],reason='failed-alone' if r['test_state']!='pass' else 'coverage-failed' if r['status']!='pass' else 'no-scope-line-covered') for r in excluded]
    csvout(EXP/'dev/dev-scope-excluded.csv',excluded_rows,[*list(original[0]),'status','scope_lines_covered','reason'])
    result=dict(methods=185,selected=len(selected),excluded=len(excluded),failed_alone=[dict(**r['method'],status=r['test_state'],message=r['message']) for r in records if r['test_state']!='pass'],coverage_failures=[r['method'] for r in records if r['status']=='coverage-fail'],per_scope_file={f:sum(f in r['scope_files_touched'] for r in selected) for f in sorted(scope)},workers=4,network_isolation='four Docker containers with independent network namespaces',retries=0,model_calls=0,seconds=round(time.time()-compile_run['prepared_unix'],3),shared_anchor_compile_seconds=compile_run['seconds'],fewer_than_20=len(selected)<20)
    write_json(RESULTS/'dev-method-coverage-t-summary.json',result)
    print(json.dumps(result),flush=True)

if __name__=='__main__':main()
