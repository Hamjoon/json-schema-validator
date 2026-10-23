"""Isolated snapshot and patched index; follows copied extractor-check drivers."""
import argparse
import collections
import csv
import hashlib
import json
import re
import shutil
import sys
import time
import types
sys.dont_write_bytecode=True
from start import EXP, REPO, RESULTS, execute, write_json
from select_scope import csvout

DS=__import__('pathlib').Path('/work/data/commit-survival/json-schema-validator')
PROJECT=DS/'projects/json-schema-validator'
INDEX=DS/'project_index'


def start_jvm():
    import jpype
    jpype.startJVM(jpype.getDefaultJVMPath(),'-Xmx4g',classpath=['/work/code/Java/project-info-process.jar','/work/code/Java/project-index-builder.jar'])


def prepare():
    PROJECT.mkdir(parents=True,exist_ok=True)
    for source,dest in [('src/main','src/main'),('src/test/java','src/test-original/java'),('src/test/resources','src/test/resources'),('src/test/suite','src/test/suite'),('target/classes','target/classes')]:
        if (REPO/source).exists():shutil.copytree(REPO/source,PROJECT/dest,dirs_exist_ok=True)
    shutil.copy2(REPO/'pom.xml',PROJECT/'pom.xml')
    attempts=[]
    for attempt in (1,2):
        if attempt==2:time.sleep(60)
        run=execute(['mvn','-q',*(['-o'] if attempt==1 else []),'dependency:copy-dependencies','-DoutputDirectory=libs','-DincludeScope=test'],PROJECT,RESULTS/f'ktester-prepare-{attempt}.txt')
        attempts.append(run)
        if run['exit_code']==0:break
    assert attempts[-1]['exit_code']==0,attempts
    jars=sorted((PROJECT/'libs').glob('*.jar'))
    assert jars
    (PROJECT/'dependencies.txt').write_text('target/classes:'+':'.join('libs/'+p.name for p in jars))
    write_json(DS/'dataset_info.json',{'json-schema-validator':{'project-name':'json-schema-validator','project-url':'projects/json-schema-validator','focal-methods':[]}})
    (DS/'.gitignore').write_text('**/target/\n**/*.jar\n__pycache__/\n')
    write_json(RESULTS/'ktester-snapshot.json',dict(attempts=attempts,jars_sha256={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in jars},jars_added_beyond_maven=0))


def index():
    import jpype
    start=time.monotonic()
    for name in ['json','codegraph','lucene']:(INDEX/name).mkdir(parents=True,exist_ok=True)
    start_jvm()
    jpype.JClass('PreProcessor').main([str(DS/'projects'),str(INDEX)])
    sys.path.insert(0,'/work/code')
    import procedure.preprocess_project as pp
    fs=types.SimpleNamespace(DATASET_PATH=str(DS),CODE_INFO_PATH=str(INDEX))
    pp.build_calling_graph(fs);pp.extract_invoke_patterns(fs)
    jpype.JClass('IndexBuilder').main(['group',str(INDEX/'json'),str(INDEX/'lucene')])
    info=json.loads((INDEX/'json/json-schema-validator.json').read_text())
    counts={part:dict(classes=len(info[part]),methods=sum(len(entries) for cls in info[part].values() for entries in cls['methods'].values()),constructors=sum(len(cls['constructors']) for cls in info[part].values())) for part in ['source','test']}
    result=dict(status='completed',seconds=round(time.monotonic()-start,3),UNREACHABLE_TARGETS=pp.UNREACHABLE_TARGETS,counts=counts)
    write_json(RESULTS/'ktester-index.json',result);print(json.dumps(result),flush=True)
    jpype.shutdownJVM()


def dataset():
    import jpype
    from renumber_dataset_ids import renumber,validate
    inventory=json.loads((RESULTS/'source-inventory.json').read_text())
    def normalized(signature):
        while '<' in signature:
            signature=re.sub(r'<[^<>]*>','',signature)
        return re.sub(r'\s+','',re.sub(r'(?:\w+\.)+(\w+)',r'\1',signature).replace('...','[]'))
    eligible={(m['class'],normalized(m['signature'])) for m in inventory['main']['methods'] if m['eligible']}
    scope=json.loads((EXP/'population/scope.json').read_text())
    start_jvm()
    extractor=jpype.JClass('extractor.SpoonExtractor')([str(PROJECT/'src/main/java')],[str(PROJECT/'libs')])
    indexed=json.loads((INDEX/'json/json-schema-validator.json').read_text())['source']
    methods=[]
    for c in scope['classes']:
        ct=extractor.getCtClass(c['class'])
        for m in sorted(ct.getMethods(),key=lambda x:x.getPosition().getLine()):
            sig=str(m.getSimpleName())+'('+', '.join(str(p.getType().getSimpleName()) for p in m.getParameters())+')'
            if (c['class'],normalized(sig)) in eligible:
                name=str(m.getSimpleName())
                candidates=[name+x['signature'].split(name,1)[1] for x in indexed[c['class']]['methods'][name] if normalized(name+x['signature'].split(name,1)[1])==normalized(sig)]
                assert len(candidates)==1,(sig,candidates)
                methods.append(c['class']+'.'+candidates[0])
    assert len(methods)==scope['m'],(len(methods),scope['m'])
    meta=[dict(project_name='json-schema-validator',put_path='/root/experiments/puts/projects/json-schema-validator',method_name_to_idx={m:i for i,m in enumerate(methods)})]
    write_json(DS/'dataset_meta.json',meta)
    jpype.JClass('DatasetPrepare').main([str(DS)])
    jpype.shutdownJVM()
    shutil.copy2(DS/'dataset_info.json',DS/'dataset_info.raw.json')
    raw=json.loads((DS/'dataset_info.raw.json').read_text());data=renumber(raw)
    checked=validate(raw,data,expected_count=scope['m'])
    write_json(DS/'dataset_info.json',data)
    for name in ['dataset_info.raw.json','dataset_info.json','dataset_meta.json']:
        shutil.copy2(DS/name,EXP/'population'/name)
    entries=data['json-schema-validator']['focal-methods']
    actual={m['class']+'.'+m['method-name'] for m in entries}
    assert actual==set(methods),(actual^set(methods))
    csvout(EXP/'population/ktester-population.csv',entries,['id','class','method-name','test-class','test-path'])
    write_json(RESULTS/'ktester-population-validation.json',checked)
    print(json.dumps(checked),flush=True)


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('stage',choices=['prepare','index','dataset']);args=parser.parse_args()
    globals()[args.stage]()
