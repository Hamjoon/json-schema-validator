"""Regenerate shared reports from four groups without rerunning original measurements."""
import argparse
import collections
import csv
import json
from start import RESULTS,write_json
import run_commit
import aggregate_survival
import aggregate_coverage
import final_check

GROUP='ouedraogo-adj'

def control():
    frozen=json.loads((RESULTS/'frozen-denominators.json').read_text())
    rows=run_commit.denominator(GROUP)
    if GROUP in frozen:assert frozen[GROUP]==rows
    else:frozen[GROUP]=rows;write_json(RESULTS/'frozen-denominators.json',frozen)
    result=run_commit.run_group(0,GROUP,'control-final')
    summary=dict(denominator=len(rows),states=dict(collections.Counter(m['state'] for m in result['methods'])),passed=all(m['state']=='pass' for m in result['methods']),seconds=result['seconds'])
    write_json(RESULTS/'control-at-t-ouedraogo-adj.json',summary)
    (RESULTS/'control-at-t-ouedraogo-adj.md').write_text('# Adjusted-group anchor control\n\n```json\n'+json.dumps(summary,indent=2)+'\n```\n')
    assert summary['passed'],'Adjusted anchor control failed'
    print(json.dumps(summary),flush=True)

def wide_tables():
    groups=run_commit.GROUPS;labels=aggregate_survival.LABELS
    losses=list(csv.DictReader((RESULTS/'first-failures.csv').open()))
    lines=['# Table 2. Breaking commits','','Cells show methods lost and first-failure kinds.','','| Index | '+' | '.join(labels[g] for g in groups)+' |','|---:|'+'---|'*len(groups)]
    for index in sorted({int(r['index']) for r in losses}):
        cells=[]
        for g in groups:
            subset=[r for r in losses if int(r['index'])==index and r['group']==g]
            kinds=collections.Counter(r['kind'] for r in subset)
            cells.append(str(len(subset))+(' ('+', '.join(f'{k}: {v}' for k,v in sorted(kinds.items()))+')' if subset else ''))
        lines.append('| '+str(index)+' | '+' | '.join(cells)+' |')
    (RESULTS/'table2-breaking-commits.md').write_text('\n'.join(lines)+'\n')
    rows=list(csv.DictReader((RESULTS/'changed-lines.csv').open()))
    lines=['# Table 4. Exposure versus break','','Exposure is coverage at c−1 of at least one executable pre-image changed line. Break is any cumulative first failure at c. Pure insertions are unexposed.','','| Exposure / break | '+' | '.join(labels[g] for g in groups)+' |','|---|'+'---:|'*len(groups)]
    counts={g:collections.Counter() for g in groups}
    for r in rows:
        for g in groups:
            pre=r[g+'_pre_image_covered'];post=r['all_'+g+'_covered']
            if pre=='' or post=='':counts[g]['unavailable']+=1
            else:counts[g][(int(pre)>0,int(r[g+'_lost'])>0)]+=1
    for label,key in [('Exposed / lost',(True,True)),('Exposed / no loss',(True,False)),('Unexposed / lost',(False,True)),('Unexposed / no loss',(False,False)),('Unavailable','unavailable')]:
        lines.append('| '+label+' | '+' | '.join(str(counts[g][key]) for g in groups)+' |')
    (RESULTS/'table4-exposure-vs-break.md').write_text('\n'.join(lines)+'\n')

def main():
    parser=argparse.ArgumentParser();parser.add_argument('stage',choices=['control','survival','coverage','verify']);args=parser.parse_args()
    run_commit.GROUPS.append(GROUP)
    if args.stage=='control':control()
    elif args.stage=='survival':aggregate_survival.main()
    elif args.stage=='coverage':aggregate_coverage.main();wide_tables()
    else:final_check.main()

if __name__=='__main__':main()
