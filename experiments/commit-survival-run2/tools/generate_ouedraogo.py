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
    parser=argparse.ArgumentParser();parser.add_argument('--round',type=int,required=True);args=parser.parse_args()
    started=time.monotonic();out=EXP/f'generated/ouedraogo/round{args.round}'
    out.mkdir(parents=True,exist_ok=True)
    sys.path.insert(0,str(SHARED/'work/python'))
    sys.path.insert(0,str(EXP/'tools/ouedraogo'))
    import generate
    from extract_jsoup import extract
    generate.EXP=out
    techniques=['ZSL','FSL','CoT','ToT','GToT']
    templates=EXP/'tools/ouedraogo/prompts/templates'
    example=(templates/'fewshot_example.txt').read_text()
    scope=json.loads((EXP/'population/scope.json').read_text())['classes']
    jobs=[]
    for cls in scope:
        name=cls['class'].rsplit('.',1)[-1];source=(REPO/cls['file']).read_text()
        values={'{class_name}':name,'{source_code}':source,'{fewshot_example}':example}
        for technique in techniques:
            template=(templates/f'{technique}.txt').read_text()
            prompt=''.join(values.get(piece,piece) for piece in re.split(r'(\{class_name\}|\{source_code\}|\{fewshot_example\})',template))
            path=out/'prompts/rendered'/name/f'{technique}.txt';path.parent.mkdir(parents=True,exist_ok=True)
            path.write_text(prompt)
            jobs.append((name,technique,hashlib.sha256(prompt.encode()).hexdigest()))
    def one(job):
        name,technique,digest=job
        result=generate.generate_one({'class':name},technique,os.environ['OPENROUTER_API_KEY'],digest)
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
    write_json(RESULTS/f'ouedraogo-round{args.round}.json',dict(round=args.round,seconds=round(time.monotonic()-started,3),prompts=len(jobs),model_calls=sum(r['attempts'] for r in rows),extracted=sum(bool(r.get('file')) for r in rows),model='openai/gpt-oss-120b',temperature=0.7,max_tokens=16384,workers=2,provider_pinned=False))


if __name__=='__main__':main()
