"""Summarize preserved compile diagnostics and author post-processing stops."""
import argparse
import collections
import json
import re
from pathlib import Path
from start import EXP, RESULTS, write_json


def main():
    parser=argparse.ArgumentParser();parser.add_argument('group');parser.add_argument('round');args=parser.parse_args()
    base=EXP/'generated'/args.group/args.round
    if args.group=='ktester':
        path=RESULTS/f'ktester-{args.round}.json';record=json.loads(path.read_text())
        log=(base/'generation.log').read_text(errors='replace')
        record['author_json_parse_error_log_entries']=log.count('occured while get json object')
        record['author_error_log_entries']=[line for line in log.splitlines() if ' ERROR ' in line]
        record['repair_attempts_by_test_class']=dict(collections.Counter(re.findall(r'try to repair test class .*/([^/\n]+)\.java',log)))
        errors=re.findall(r'ERROR Error in post process: (.+)',log)
        if errors:
            targets=re.findall(r'try to repair test class .*/([^/\n]+)\.java',log)
            data=json.loads((EXP/'population/dataset_info.json').read_text())['json-schema-validator']['focal-methods']
            last=targets[-1] if targets else None
            indices=[i for i,m in enumerate(data,1) if Path(m['test-path']).stem==last]
            record.update(pipeline_status='generation-completed-repair-incomplete',repair_error=errors[-1],repair_targets=len(data),repair_stopped_at_target=indices[0] if len(indices)==1 else None,repair_last_logged_class=last,repair_index_evidence='Last try-to-repair class before author-caught error; matched to ordered dataset')
        write_json(path,record)
        print(json.dumps(record),flush=True)
        return
    counts=collections.Counter();records=[]
    for p in sorted((base/'validation').glob('*.json')):
        r=json.loads(p.read_text());compile=r.get('compile');parse=r['parse']
        if compile and compile['exit_code']==0:continue
        source=EXP/r['file']
        log=EXP/(compile['log'] if compile else parse['log'])
        diagnostics=log.read_text(errors='replace')
        categories=[]
        patterns={'missing-package-or-import':r'package .+ does not exist|import .*\n.*\^',
                  'unresolved-symbol':r'cannot find symbol',
                  'inaccessible-member-or-constructor':r'has (?:private|protected) access|cannot be accessed from outside package',
                  'method-or-constructor-signature':r'cannot be applied to given types|no suitable (?:method|constructor) found',
                  'incompatible-types':r'incompatible types',
                  'abstract-or-override-contract':r'is abstract; cannot be instantiated|does not override|does not implement|cannot override',
                  'unhandled-checked-exception':r'unreported exception',
                  'duplicate-declaration':r'is already defined',
                  'public-class-filename':r'should be declared in a file named',
                  'syntax':r"';' expected|illegal start|reached end|not a statement|unclosed"}
        for name,pattern in patterns.items():
            if re.search(pattern,diagnostics):categories.append(name)
        if not categories:categories=['other-compile' if compile else 'parse-failure']
        counts.update(categories)
        records.append(dict(file=r['file'],technique=source.parent.name,categories=categories,log=str(log.relative_to(EXP)),diagnostics=diagnostics))
    result=dict(round=args.round,failed_files=len(records),category_file_counts=dict(counts),categories_may_overlap=True,files=records,prompts_changed=False,generated_sources_repaired=False)
    write_json(RESULTS/f'ouedraogo-{args.round}-compile-failures.json',result)
    print(json.dumps({k:v for k,v in result.items() if k!='files'}),flush=True)


if __name__=='__main__':main()
