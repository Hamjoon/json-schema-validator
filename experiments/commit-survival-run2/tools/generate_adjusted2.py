"""Apply the copied five prompt templates and generation function to scope classes."""
import argparse
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import time
from start import EXP, REPO, RESULTS, SHARED, write_json
from select_scope import csvout


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--round',type=int,required=True);parser.add_argument('--attempt',type=int,choices=[1,2],default=1);args=parser.parse_args()
    label=f'round{args.round}'+('-retry' if args.attempt==2 else '')
    started=time.monotonic();out=EXP/'generated/ouedraogo-adj2'/label
    assert args.round==1 and args.attempt==1, 'Exactly one round'
    assert not (out/'runs').exists(), 'Never repeat paid calls'
    from check_adjusted2_blocks import check
    check(write=False)
    out.mkdir(parents=True,exist_ok=True)
    sys.path.insert(0,str(SHARED/'work/python'))
    sys.path.insert(0,str(EXP/'tools/ouedraogo'))
    import generate
    from extract_jsoup import extract
    generate.EXP=out
    jobs=[(r['class_name'],r['technique'],r['sha256']) for r in json.loads((out/'prompts/manifest.json').read_text())]
    assert len(jobs)==25
    def one(job):
        name,technique,digest=job
        result=generate.generate_one({'class':name},technique,os.environ['OPENROUTER_API_KEY'],digest,authorized_attempt=1)
        response=out/result['response_path']
        extracted=extract(response.read_text())
        result['extraction']={k:v for k,v in extracted.items() if k!='combine_v2'}
        if extracted['csr_v2']:
            path=out/'sources'/name/technique/(extracted['class_name']+'.java');path.parent.mkdir(parents=True,exist_ok=True)
            path.write_text(extracted['combine_v2'])
            result['file']=str(path.relative_to(EXP))
        write_json(out/'records'/f'{name}-{technique}.json',result)
        print(json.dumps(dict(class_name=name,technique=technique,status=result['status'],extracted=bool(result.get('file')))),flush=True)
        return result
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        rows=list(pool.map(one,jobs))
    write_json(out/'generation.json',rows)
    write_json(RESULTS/f'ouedraogo-adj2-{label}.json',dict(round=args.round,attempt=args.attempt,seconds=round(time.monotonic()-started,3),prompts=len(jobs),model_calls=sum(r['attempts'] for r in rows),extracted=sum(bool(r.get('file')) for r in rows),model='openai/gpt-oss-120b',temperature=0.7,max_tokens=16384,workers=2,provider_pinned=False))


if __name__=='__main__':main()
