"""Run released KTester with isolated paths and in-memory credential injection."""
import argparse
import hashlib
import itertools
import json
import logging
import os
from pathlib import Path
import shutil
import subprocess
import sys
import threading
import time
sys.dont_write_bytecode=True
from start import EXP, RESULTS, write_json


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--round',type=int,required=True);parser.add_argument('--attempt',type=int,choices=[1,2],default=1);args=parser.parse_args()
    label=f'round{args.round}'+('-retry' if args.attempt==2 else '')
    ds=Path('/work/data/commit-survival/json-schema-validator')
    assert args.round == 1, 'Run 2 allows one KTester round only'
    out=ds/'rounds'/('run2-'+label)
    assert not out.exists(),'Refuse replacing an existing model round'
    out.mkdir(parents=True)
    os.chdir('/work/code');sys.path.insert(0,'/work/code')
    from settings import FileStructure as FS, TaskSettings as TS, LLMSettings as LS, TimeRecord as TR
    FS.DATASET_PATH='../data/commit-survival/json-schema-validator'
    FS.CODE_INFO_PATH=FS.DATASET_PATH+'/project_index'
    for field,folder in [('PROMPT_PATH','context+prompts'),('FIX_PATH','fix'),('RESPONSE_PATH','responses'),('TESTCLASSS_PATH','test_classes'),('REPORT_PATH','reports')]:
        setattr(FS,field,str(out/'<project>'/folder))
    TR.TIME_RECORD_PATH=str(out/'time')
    TS.PROJECTS=['json-schema-validator']
    assert TS.FIX_TRIES==5 and TS.MAX_WORKERS==8 and LS.TEMPERATURE==0.5 and LS.MODEL=='openai/gpt-oss-120b'
    key=os.environ['OPENROUTER_API_KEY']
    LS.API_ACCOUNTS=[dict(base_url='https://openrouter.ai/api/v1',api_key=key)]
    # Capture every API request/response, excluding all authorization headers.
    import openai
    from openai.resources.chat.completions import Completions
    from tenacity import wait_fixed,stop_after_attempt
    original_init=openai.OpenAI.__init__
    def initialize(self,*a,**kw):
        kw['max_retries']=0;kw['timeout']=600
        return original_init(self,*a,**kw)
    openai.OpenAI.__init__=initialize
    original_create=Completions.create
    counter=itertools.count(1);lock=threading.Lock()
    raw=out/'raw-responses';raw.mkdir()
    def create(self,*a,**kw):
        with lock:number=next(counter)
        call=raw/f'{number:04d}';call.mkdir()
        body={k:v for k,v in kw.items() if k in ['model','messages','temperature','max_tokens','response_format'] and not isinstance(v,(openai.Omit,openai.NotGiven))}
        write_json(call/'request.json',body)
        start=time.monotonic()
        try:
            response=original_create(self,*a,**kw)
            data=response.model_dump_json()
            if key in data:raise RuntimeError('Credential echoed by server; response not saved')
            (call/'response.json').write_text(data+'\n')
            write_json(call/'call.json',dict(status='ok',seconds=round(time.monotonic()-start,3)))
            return response
        except Exception as exc:
            write_json(call/'call.json',dict(status='error',error_type=type(exc).__name__,seconds=round(time.monotonic()-start,3)))
            raise
    Completions.create=create
    from tools.llm_api import LLMCaller
    LLMCaller._generation=LLMCaller._generation.retry_with(wait=wait_fixed(60),stop=stop_after_attempt(2))
    original_run=subprocess.run
    def bounded(command,*a,**kw):
        if isinstance(command,list) and command:
            binary=Path(str(command[0])).name
            limit={'java':120,'javac':300,'mvn':600}.get(binary)
            if limit:kw['timeout']=min(kw.get('timeout') or limit,limit)
        return original_run(command,*a,**kw)
    subprocess.run=bounded
    logging.basicConfig(filename=str(out/'generation.log'),level=logging.INFO,format='%(asctime)s %(levelname)s %(message)s')
    import jpype
    import preparation
    preparation.set_file_structure()
    jpype.startJVM(jpype.getDefaultJVMPath(),'-Xmx4g',classpath=['./Java/project-info-process.jar','./Java/project-index-builder.jar'])
    start=time.monotonic();status='completed';error=None
    try:
        import generate_unit_test
        generate_unit_test.run()
    except Exception as exc:
        status='failed';error=type(exc).__name__
        logging.exception('KTester round failed')
    finally:
        jpype.shutdownJVM()
        result=dict(round=args.round,status=status,error_type=error,seconds=round(time.monotonic()-start,3),model_calls=len(list(raw.glob('*/call.json'))),model=LS.MODEL,temperature=LS.TEMPERATURE,fix_tries=TS.FIX_TRIES,workers=TS.MAX_WORKERS,prompts_unmodified=True,provider_pinned=False)
        write_json(out/'run.json',result)
        destination=EXP/'generated/ktester'/label
        # Never archive credentials, jars, or compiled classes.
        shutil.copytree(out,destination,ignore=shutil.ignore_patterns('*.class','*.jar'),dirs_exist_ok=True)
        project=ds/'projects/json-schema-validator'
        if (project/'src/test/java').exists():shutil.copytree(project/'src/test/java',destination/'sources',dirs_exist_ok=True)
        for p in destination.rglob('*'):
            if p.is_file() and key.encode() in p.read_bytes():
                p.write_bytes(p.read_bytes().replace(key.encode(),b'[REDACTED]'))
                result['credential_redactions']=result.get('credential_redactions',0)+1
        write_json(RESULTS/f'ktester-{label}.json',result)
        print(json.dumps(result),flush=True)


if __name__=='__main__':main()
