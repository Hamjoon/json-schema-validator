"""Compile each generated file; require 3/3 passing runs; deduplicate stable bodies."""
import argparse
import collections
import concurrent.futures
import csv
import json
from pathlib import Path
import time
from start import EXP, REPO, RESULTS, execute, write_json
from select_scope import csvout
from run_commit import run_file


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--group',choices=['ktester','ouedraogo'],required=True);parser.add_argument('--round',required=True);args=parser.parse_args()
    start=time.monotonic();base=EXP/'generated'/args.group/args.round
    sources=base/'json-schema-validator/test_classes' if args.group=='ktester' else base/'sources'
    records=[]
    # Parse each source independently so a malformed file cannot discard others.
    def validate(pair):
        number,source=pair;work=EXP/f'work/validation/{args.group}/{args.round}/{number}';work.mkdir(parents=True,exist_ok=True)
        log=work/'inventory.tsv'
        # SourceInventory accepts a file as a Files.walk root too.
        parse=execute(['java','-Xmx512m',str(EXP/'tools/SourceInventory.java'),str(source)],REPO,log,300)
        members=[]
        if parse['exit_code']==0:
            for line in log.read_text().splitlines():
                parts=line.split('\t')
                if parts[0]=='M' and parts[3]:
                    _,cls,method,kind,eligible,signature,display,lineno,body_hash=parts
                    file=str(source.relative_to(EXP))
                    members.append(dict(file=file,**{'class':cls},method=method,kind=kind,id='|'.join([file,cls,method]),body_hash=body_hash,round=args.round,technique=source.parent.name if args.group=='ouedraogo' else 'KTester'))
        if members:
            result=run_file(0,args.group,str(source.relative_to(EXP)),members,work/'test',repetitions=3,retry=False)
        else:
            result=dict(file=str(source.relative_to(EXP)),methods=[],compile=None,runs=[])
        result['parse']=parse
        write_json(base/'validation'/f'{number}.json',result)
        print(json.dumps(dict(group=args.group,file=source.name,methods=len(members),passed=sum(m['state']=='pass' for m in result['methods']))),flush=True)
        return result
    paths=sorted(sources.glob('*.java') if args.group=='ktester' else sources.rglob('*.java')) if sources.exists() else []
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:records=list(pool.map(validate,enumerate(paths)))
    denompath=RESULTS/f'llm-{args.group}-denominator.csv'
    prior=list(csv.DictReader(denompath.open())) if denompath.exists() else []
    assert not any(r['round']==args.round for r in prior),'Round already validated'
    seen={r['body_hash'] for r in prior};kept=list(prior);duplicate=0;stable=0
    for record in records:
        for row in record['methods']:
            if row['state']!='pass':continue
            stable+=1
            if row['body_hash'] in seen:duplicate+=1;row['duplicate']=True;continue
            seen.add(row['body_hash']);kept.append(row)
    fields=['file','class','method','kind','body_hash','round','technique']
    csvout(denompath,kept,fields)
    funnel=dict(group=args.group,round=args.round,files=len(paths),compiled=sum(bool(r['compile']) and r['compile']['exit_code']==0 for r in records),methods=sum(len(r['methods']) for r in records),stable_3_of_3=stable,duplicates=duplicate,kept_this_round=stable-duplicate,total_kept=len(kept),seconds=round(time.monotonic()-start,3),per_prompt={})
    for tech in ['KTester'] if args.group=='ktester' else ['ZSL','FSL','CoT','ToT','GToT']:
        fs=[r for r in records if (Path(r['file']).parent.name==tech or args.group=='ktester')]
        funnel['per_prompt'][tech]=dict(files=len(fs),compiled=sum(bool(r['compile']) and r['compile']['exit_code']==0 for r in fs),methods=sum(len(r['methods']) for r in fs),stable=sum(m['state']=='pass' for r in fs for m in r['methods']))
    write_json(base/'funnel.json',funnel)
    (base/'funnel.md').write_text('# Generation funnel\n\n```json\n'+json.dumps(funnel,indent=2)+'\n```\n\nExact whitespace-stripped body hashes are deduplicated within and across rounds. Original source files remain preserved; only kept methods enter the denominator and method selections.\n')
    print(json.dumps(funnel),flush=True)


if __name__=='__main__':main()
