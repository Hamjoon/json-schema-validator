"""Three-group adaptation of the copied Codec class-isolated javac/JUnit runner."""
import collections
import csv
import hashlib
import json
from pathlib import Path
import re
import shutil
import time
import xml.etree.ElementTree as ET
from start import EXP, REPO, RESULTS, execute, write_json
from select_scope import csvout

GROUPS=['dev','ktester','ouedraogo']


def denominator(group):
    path=EXP/'dev/dev-denominator.csv' if group=='dev' else RESULTS/f'llm-{group}-denominator.csv'
    rows=list(csv.DictReader(path.open()))
    for r in rows:
        if group=='dev':r['file']='dev/src-test-t/java/'+r['class'].replace('.','/')+'.java'
        r['id']='|'.join(r[k] for k in ['file','class','method'])
    assert len({r['id'] for r in rows})==len(rows)
    return rows


def classpath(index):
    records=json.loads((EXP/'tools/classpath-artifacts.json').read_text())
    jars=sorted([r for r in records if r['role']=='test'],key=lambda r:r['filename'].startswith('junit-platform-console-standalone'))
    testjars=[str(EXP/'work/libs'/r['filename']) for r in jars]
    folder=REPO if index==0 else EXP/f'checkouts/{index}'
    cpfile=EXP/'work/classpath-t.txt' if index==0 else EXP/json.loads((RESULTS/f'builds/{index}.json').read_text())['classpath']
    projectjars=[p for p in cpfile.read_text().strip().split(':') if p and not any(s in p for s in ['/org/junit/','/junit/junit/','/org/opentest4j/','/org/apiguardian/'])]
    return folder,':'.join([str(folder/'target/classes'),*testjars,*projectjars])


def parse_reports(folder):
    observations=collections.defaultdict(list)
    for path in sorted(folder.glob('TEST-*.xml')):
        for case in ET.parse(path).getroot().iter('testcase'):
            output='\n'.join(n.text or '' for n in case.findall('system-out'))
            match=re.search(r'\[(?:method|test-template|test-factory):([^\]]+)\]',output)
            label=match.group(1) if match else case.get('name','')
            name=re.match(r'^([A-Za-z_$][\w$]*)(?:\(|\[|$)',label)
            if not name:continue
            problem=case.find('error')
            if problem is None:problem=case.find('failure')
            kind=problem.get('type','') if problem is not None else ''
            state='absent' if case.find('skipped') is not None else ('fail' if 'Assertion' in kind or kind.endswith(('ComparisonFailure','MultipleFailuresError')) else 'error') if problem is not None else 'pass'
            observations[(case.get('classname'),name.group(1))].append(dict(state=state,message=(problem.get('message','') or problem.text or '')[:4000] if problem is not None else '',raw_name=case.get('name')))
    return observations


def method_state(row,observations,run):
    if run['timeout']:return 'timeout','Class run exceeded 120 seconds'
    found=observations.get((row['class'],row['method']),[])
    if not found:return 'absent','Method not reported by launcher'
    for state in ['error','fail','absent']:
        for item in found:
            if item['state']==state:return state,item['message']
    return 'pass',''


def selectors(rows,runtime,folder):
    selected={}
    for cls in sorted({r['class'] for r in rows}):
        log=folder/('selectors-'+cls+'.txt')
        run=execute(['java','-Xmx1g','-cp',str(EXP/'work/runner-tools')+':'+runtime,'MethodSelectors',cls],REPO,log,120)
        choices=collections.defaultdict(list)
        if run['exit_code']==0:
            for line in log.read_text().splitlines():
                if '\t' in line:
                    name,selector=line.split('\t',1);choices[name].append(selector)
        for row in rows:
            if row['class']==cls:selected[row['id']]=sorted(set(choices[row['method']]))
    return selected


def run_file(index,group,filename,members,part,coverage_file=None,repetitions=1,retry=True):
    folder,cp=classpath(index)
    part.mkdir(parents=True,exist_ok=True)
    classes=part/'classes';classes.mkdir(exist_ok=True)
    source=EXP/filename
    sourcepath=EXP/'dev/src-test-t/java' if group=='dev' else source.parent
    compile_run=execute(['javac','-J-Xmx512m','-cp',cp,'-sourcepath',str(sourcepath),'-d',str(classes),str(source)],folder,part/'javac.txt',300)
    record=dict(file=filename,compile=compile_run,runs=[],methods=[])
    if compile_run['exit_code']:
        record['methods']=[{**r,'state':'compile-fail','flaky':False,'message':(part/'javac.txt').read_text(errors='replace')[:4000]} for r in members]
        return record
    runtime=':'.join([str(classes),cp,str(EXP/'dev/src-test-t/resources'),str(EXP/'dev/src-test-t/suite')])
    selection=selectors(members,runtime,part)
    base=['java','-Xmx1g','-XX:+UseSerialGC']
    if coverage_file:base+=[f'-javaagent:{EXP}/work/libs/org.jacoco.agent-0.8.12-runtime.jar=destfile={coverage_file},append=true']
    base+=['-cp',runtime,'org.junit.platform.console.ConsoleLauncher','--disable-banner','--disable-ansi-colors','--fail-if-no-tests','--details','summary']
    def launch(chosen,label):
        reports=part/label;reports.mkdir(exist_ok=True)
        args=[]
        for r in chosen:
            for selector in selection[r['id']]:args+=['--select-method',selector]
        if not args:
            return dict(timeout=False,exit_code=2,seconds=0,selection='no compiled selector'),{}
        run=execute([*base,*args,'--reports-dir',str(reports)],folder,reports/'console.txt',120)
        record['runs'].append(run)
        return run,parse_reports(reports)
    runs=[launch(members,'run'+str(i)) for i in range(1,repetitions+1)]
    for row in members:
        states=[method_state(row,obs,run) for run,obs in runs]
        state,message=next(((s,m) for s,m in states if s!='pass'),('pass',''))
        result={**row,'state':state,'message':message,'flaky':False,'initial_state':state,'selector':selection[row['id']],'baseline_states':[s for s,_ in states]}
        if retry and state in ['fail','error']:
            rerun,obs=launch([row],'retry-'+row['method'])
            retry_state,retry_message=method_state(row,obs,rerun)
            result.update(retry_state=retry_state,retry_message=retry_message)
            if retry_state=='pass':result.update(state='pass',flaky=True)
        record['methods'].append(result)
    return record


def line_rows(xml):
    rows=[]
    for package in ET.parse(xml).getroot().findall('package'):
        for source in package.findall('sourcefile'):
            path='src/main/java/'+package.get('name')+'/'+source.get('name')
            for line in source.findall('line'):
                rows.append(dict(file=path,line=int(line.get('nr')),missed=int(line.get('mi')),covered=int(line.get('ci'))))
    return sorted(rows,key=lambda r:(r['file'],r['line']))


def run_group(index,group,phase='a',selected_ids=None):
    started=time.monotonic();rows=denominator(group)
    if selected_ids is not None:rows=[r for r in rows if r['id'] in set(selected_ids)]
    output=RESULTS/('raw' if phase=='a' else 'raw-coverage' if phase=='b' else phase)/f'{index}-{group}.json'
    fingerprint=hashlib.sha256(json.dumps([(r['id'],hashlib.sha256((EXP/r['file']).read_bytes()).hexdigest()) for r in rows],sort_keys=True).encode()).hexdigest()
    if output.exists():
        old=json.loads(output.read_text());assert old['input_sha256']==fingerprint
        return old
    work=EXP/f'work/phase-{phase}/{index}/{group}'
    if work.exists():shutil.rmtree(work)
    work.mkdir(parents=True)
    coverage=phase=='b';execfile=work/'jacoco.exec'
    by_file=collections.defaultdict(list)
    for r in rows:by_file[r['file']].append(r)
    files=[]
    for number,(filename,members) in enumerate(sorted(by_file.items())):
        files.append(run_file(index,group,filename,members,work/str(number),execfile if coverage else None,retry=phase in ['a','b']))
    commit=json.loads((RESULTS/'anchor.json').read_text()) if index==0 else next(r for r in csv.DictReader((RESULTS/'commits.csv').open()) if int(r['index'])==index)
    result=dict(index=index,sha=commit['sha'],group=group,phase=phase,status='evaluated',input_sha256=fingerprint,files=files,methods=[r for f in files for r in f['methods']],requested_ids=[r['id'] for r in rows])
    if coverage:
        folder,_=classpath(index);xml=RESULTS/f'coverage/{index}-{group}.xml';xml.parent.mkdir(parents=True,exist_ok=True)
        run=execute(['java','-Xmx1g','-jar',str(EXP/'work/libs/org.jacoco.cli-0.8.12-nodeps.jar'),'report',*([str(execfile)] if execfile.exists() else []),'--classfiles',str(folder/'target/classes'),'--sourcefiles',str(folder/'src/main/java'),'--xml',str(xml)],folder,work/'coverage-report.txt',300)
        result['coverage_report']=run
        if run['exit_code']==0:
            csvout(xml.with_name(xml.stem+'-lines.csv'),line_rows(xml),['file','line','missed','covered'])
        else:result['status']='coverage-fail'
    assert len(result['methods'])==len(rows)
    result['seconds']=round(time.monotonic()-started,3)
    write_json(output,result)
    return result
