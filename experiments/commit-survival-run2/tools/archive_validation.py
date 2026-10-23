"""Preserve generated-test javac and three-run JUnit evidence beside each round."""
import argparse
import json
import shutil
from start import EXP,write_json


def archive(base,number,record):
    group=base.parent.name;label=base.name
    work=EXP/f'work/validation/{group}/{label}/{number}'
    destination=base/'validation-logs'/str(number)
    shutil.copytree(work,destination,dirs_exist_ok=True,ignore=shutil.ignore_patterns('classes','*.class','*.jar'))
    old=str(work.relative_to(EXP));new=str(destination.relative_to(EXP))
    def remap(value):
        if isinstance(value,str):return new+value[len(old):] if value.startswith(old+'/') else value
        if isinstance(value,list):return [remap(x) for x in value]
        if isinstance(value,dict):return {k:remap(v) for k,v in value.items()}
        return value
    record=remap(record)
    write_json(base/'validation'/f'{number}.json',record)
    return record


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--group',required=True);parser.add_argument('--round',required=True);args=parser.parse_args()
    base=EXP/'generated'/args.group/args.round
    for path in sorted((base/'validation').glob('*.json')):archive(base,path.stem,json.loads(path.read_text()))
    print('Archived compiler and JUnit evidence for '+args.group+'/'+args.round)
