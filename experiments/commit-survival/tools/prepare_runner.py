"""Copy verified prior-run test jars and resolve the explicit JUnit 4 additions."""
import hashlib
import json
import shutil
import time
from pathlib import Path
from start import EXP, REPO, RESULTS, execute, write_json


def main():
    runtime=EXP/'work/runtime'
    for name in ['resources','suite']:
        source=EXP/'dev/src-test-t'/name
        if source.exists():shutil.copytree(source,runtime/'src/test'/name,dirs_exist_ok=True)
    libs=EXP/'work/libs';libs.mkdir(parents=True,exist_ok=True)
    records=json.loads((EXP/'tools/upstream/classpath-artifacts.json').read_text())
    for r in records:
        src=Path('/codec/experiments/ktester-commit-survival/work/libs')/r['filename']
        if not src.exists():
            group,artifact,version=r['coordinate'].split(':')
            src=Path('/root/.m2/repository')/group.replace('.','/')/artifact/version/r['filename']
            if not src.exists():
                coordinate=r['coordinate']+(':jar:'+r['classifier'] if r.get('classifier') else '')
                for attempt in (1,2):
                    if attempt==2:time.sleep(60)
                    run=execute(['mvn','-q','dependency:get','-Dartifact='+coordinate,'-Dtransitive=false'],REPO,RESULTS/f'runner-dependency-{artifact}-{attempt}.txt')
                    if run['exit_code']==0:break
                assert src.exists(),run
        shutil.copy2(src,libs/r['filename'])
        digest=hashlib.sha256(src.read_bytes()).hexdigest()
        assert not r.get('supplied_sha256') or r['supplied_sha256']==digest
        r['sha256']=digest;r['source']=str(src)
    for coordinate in ['org.junit.vintage:junit-vintage-engine:5.10.2','junit:junit:4.13.2','org.hamcrest:hamcrest-core:1.3']:
        group,artifact,version=coordinate.split(':')
        jar=Path('/root/.m2/repository')/group.replace('.','/')/artifact/version/f'{artifact}-{version}.jar'
        if not jar.exists():
            for attempt in (1,2):
                if attempt==2:time.sleep(60)
                run=execute(['mvn','-q','dependency:get','-Dartifact='+coordinate,'-Dtransitive=false'],REPO,RESULTS/f'runner-dependency-{artifact}-{attempt}.txt')
                if run['exit_code']==0:break
            assert run['exit_code']==0,run
        shutil.copy2(jar,libs/jar.name)
        records.append(dict(filename=jar.name,coordinate=coordinate,role='test',sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),source=str(jar)))
    write_json(EXP/'tools/classpath-artifacts.json',records)
    helper=EXP/'work/runner-tools';helper.mkdir(parents=True,exist_ok=True)
    result=execute(['javac','-J-Xmx512m','-d',str(helper),str(EXP/'tools/upstream/MethodSelectors.java')],REPO,RESULTS/'runner-helper-compile.txt',300)
    assert result['exit_code']==0,result
    # Anchor project dependency list; keep project libraries separate from fixed runner jars.
    cp=EXP/'work/classpath-t.txt'
    for attempt in (1,2):
        if attempt==2:time.sleep(60)
        run=execute(['mvn','-q',*(['-o'] if attempt==1 else []),'dependency:build-classpath','-DincludeScope=test','-Dmdep.outputFile='+str(cp)],REPO,RESULTS/f'runner-classpath-t-{attempt}.txt')
        if run['exit_code']==0:break
    assert cp.exists(),run
    write_json(RESULTS/'runner-setup.json',dict(jars=len(records),helper=result,classpath=run))


if __name__=='__main__':main()
