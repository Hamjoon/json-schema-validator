"""Changed-line coverage and pre-image exposure from recorded JaCoCo line CSVs."""
import collections
import csv
import json
from start import EXP, RESULTS, write_json
from select_scope import csvout
from run_commit import GROUPS
from aggregate_survival import LABELS,md,pct


def flatten(image):return {(file,int(line)) for file,lines in image.items() for line in lines}


def main():
    commits=list(csv.DictReader((RESULTS/'commits.csv').open()))
    curves={int(r['index']):r for r in csv.DictReader((RESULTS/'survival.csv').open())}
    scope={r['file'] for r in json.loads((EXP/'population/scope.json').read_text())['classes']}
    indices=[int(r['index']) for r in csv.DictReader((RESULTS/'coverage-commits.csv').open())]
    maps={};missing=[];identical=[]
    for i in indices:
        maps[i]={}
        for g in GROUPS:
            path=RESULTS/f'coverage/{i}-{g}-lines.csv'
            if not path.exists():maps[i][g]=None;missing.append(dict(index=i,group=g));continue
            lines=list(csv.DictReader(path.open()))
            data={(r['file'],int(r['line'])):int(r['covered'])>0 for r in lines}
            assert len(data)==len(lines),(i,g,'duplicate lines')
            maps[i][g]=data
        available=[set(m) for m in maps[i].values() if m is not None]
        assert not available or all(m==available[0] for m in available),(i,'different executable universe')
        if len(available)==3:identical.append(i)
    rows=[];exposure={g:collections.Counter() for g in GROUPS};unknown=collections.Counter();zero=0;unavailable=0
    for commit in commits:
        if commit['changes-src-main']!='yes':continue
        i=int(commit['index']);change=json.loads((RESULTS/f'changes/{i}.json').read_text());post=flatten(change['post_image']);pre=flatten(change['pre_image'])
        current=maps[i];previous=maps[i-1];available=[m for m in current.values() if m is not None]
        row={k:commit[k] for k in ['index','sha','date','subject']}
        for category,changed in [('all',post),('scope',{p for p in post if p[0] in scope}),('other',{p for p in post if p[0] not in scope})]:
            executable=changed&available[0].keys() if available else None
            n=len(executable) if executable is not None else ''
            row[category+'_executable']=n
            for g in GROUPS:
                count=sum(current[g][p] for p in executable) if executable is not None and current[g] is not None else ''
                row[category+'_'+g+'_covered']=count
                row[category+'_'+g+'_percent']=round(100*count/n,3) if n not in ['',0] and count!='' else ''
        for g in GROUPS:
            row[g+'_lost']=int(curves[i][g+'_lost'])
            before=previous[g]
            count=sum(before[p] for p in pre&before.keys()) if before is not None else ''
            row[g+'_pre_image_covered']=count
            if count=='' or current[g] is None or curves[i]['evaluated']!='yes':unknown[g]+=1
            else:exposure[g][(count>0,row[g+'_lost']>0)]+=1
        if row['all_executable']=='':unavailable+=1
        elif row['all_executable']==0:zero+=1
        rows.append(row)
    csvout(RESULTS/'changed-lines.csv',rows,list(rows[0]) if rows else ['index'])
    lines=['# Table 3. Changed-line coverage','','Coverage uses methods alive entering each index. Changed-line coordinates are the post-image added/modified lines. Build failures and missing coverage remain unavailable.','']
    totals={}
    for category in ['all','scope','other']:
        lines += ['## '+category.capitalize()+' files','','| Index | Subject | Executable lines | Dev covered | KTester covered | Ouédraogo covered | Dev lost | KTester lost | Ouédraogo lost |','|---:|---|---:|---:|---:|---:|---:|---:|---:|']
        visible=[r for r in rows if r[category+'_executable'] not in ['',0]]
        for row in visible:
            n=row[category+'_executable'];cells=[row['index'],md(row['subject']),str(n)]
            cells += [pct(row[category+'_'+g+'_covered'],n) if row[category+'_'+g+'_covered']!='' else 'unavailable' for g in GROUPS]
            cells += [str(row[g+'_lost']) for g in GROUPS]
            lines.append('| '+' | '.join(cells)+' |')
        common=[r for r in visible if all(r[category+'_'+g+'_covered']!='' for g in GROUPS)]
        n=sum(r[category+'_executable'] for r in common)
        counts={g:sum(r[category+'_'+g+'_covered'] for r in common) for g in GROUPS}
        totals[category]=dict(executable=n,**counts)
        lines.append('| Total | Common measured commits | '+str(n)+' | '+' | '.join(pct(counts[g],n) for g in GROUPS)+' | — | — | — |')
        lines.append('')
    lines.append(f'Production-change commits: {len(rows)}. Commits with zero executable changed lines: {zero}. Commits without an executable-line measurement: {unavailable}.')
    (RESULTS/'table3-changed-line-coverage.md').write_text('\n'.join(lines)+'\n')
    lines=['# Table 4. Exposure versus break','','Exposure is coverage at c−1 of at least one executable pre-image changed line. A pure insertion has no pre-image changed lines and is unexposed by this definition. Break is any cumulative first failure at c.','']
    for g in GROUPS:
        lines += ['## '+LABELS[g],'','| Changed lines covered at c−1 | Lost at c | No loss at c |','|---|---:|---:|',f'| Yes | {exposure[g][True,True]} | {exposure[g][True,False]} |',f'| No | {exposure[g][False,True]} | {exposure[g][False,False]} |','',f'Unavailable observations: {unknown[g]}.','']
        assert sum(exposure[g].values())+unknown[g]==len(rows)
    (RESULTS/'table4-exposure-vs-break.md').write_text('\n'.join(lines)+'\n')
    lines=['# Coverage at t','','| Scope file | Executable lines | Dev | KTester | Ouédraogo |','|---|---:|---:|---:|---:|']
    baseline={};available=[m for m in maps[0].values() if m is not None]
    for file in sorted(scope):
        keys={p for p in available[0] if p[0]==file} if available else set()
        counts={g:sum(maps[0][g][p] for p in keys) if maps[0][g] is not None else None for g in GROUPS}
        baseline[file]=dict(executable=len(keys),**counts)
        lines.append('| '+file+' | '+str(len(keys))+' | '+' | '.join(pct(counts[g],len(keys)) if counts[g] is not None else 'unavailable' for g in GROUPS)+' |')
    (RESULTS/'coverage-t.md').write_text('\n'.join(lines)+'\n')
    result=dict(coverage_indices=len(indices),production_changes=len(rows),identical_executable_line_sets=identical,missing=missing,zero_executable_change_commits=zero,unavailable_commits=unavailable,totals=totals,baseline=baseline)
    write_json(RESULTS/'coverage-consistency.json',result)
    with (RESULTS/'consistency.md').open('a') as out:
        out.write(f'\nCoverage: identical executable line sets across all three groups at {len(identical)} indices. Required indices: {len(indices)}. Missing measurements: {len(missing)} (enumerated in coverage-consistency.json).\n')
    print(json.dumps({k:v for k,v in result.items() if k not in ['baseline','identical_executable_line_sets','missing']}),flush=True)


if __name__=='__main__':main()
