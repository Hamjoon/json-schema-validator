"""Cumulative fixed-denominator survival derived only from recorded results."""
import collections
import csv
import json
import sys
from start import EXP, RESULTS, SHARED, write_json
from select_scope import csvout
from run_commit import GROUPS,denominator

LABELS={'dev':'Dev','ktester':'KTester','ouedraogo':'Ouédraogo','ouedraogo-adj':'Ouédraogo (adjusted)'}


def md(value):return str(value).replace('|','\\|').replace('\n',' ')
def pct(n,d):return f'{n} ({100*n/d:.1f}%)' if d else f'{n} (n/a)'


def main():
    commits=list(csv.DictReader((RESULTS/'commits.csv').open()))
    anchor=json.loads((RESULTS/'anchor.json').read_text())
    zero=dict(index='0',sha=anchor['sha'],date=anchor['date'],subject='t = 1.1.0')
    denominators=json.loads((RESULTS/'frozen-denominators.json').read_text())
    write_json(RESULTS/'denominators.json',{g:len(rows) for g,rows in denominators.items()})
    alive={g:{r['id'] for r in rows} for g,rows in denominators.items()}
    entering={};curves=[];losses=[];missing=[];failed=[]
    for commit in [zero,*commits]:
        i=int(commit['index']);entering[str(i)]={g:sorted(ids) for g,ids in alive.items()}
        build_ok=i==0 or json.loads((RESULTS/f'builds/{i}.json').read_text())['build']=='ok'
        if not build_ok:failed.append(i)
        record={**commit,'evaluated':'yes' if build_ok else 'no'}
        for g in GROUPS:
            lost=[]
            if i and build_ok:
                path=RESULTS/f'raw/{i}-{g}.json'
                if not path.exists():missing.append(dict(index=i,group=g));record['evaluated']='incomplete'
                else:
                    raw=json.loads(path.read_text());expected={r['id'] for r in denominators[g]}
                    assert {r['id'] for r in raw['methods']}==expected,(i,g)
                    for m in raw['methods']:
                        if m['id'] in alive[g] and m['state']!='pass':
                            lost.append(m);alive[g].remove(m['id'])
                            losses.append(dict(index=i,sha=commit['sha'],group=g,id=m['id'],**{'class':m['class']},method=m['method'],kind=m['state'],flaky=m['flaky']))
            record[g+'_alive']=len(alive[g]);record[g+'_lost']=len(lost)
        curves.append(record)
    fields=['index','sha','date','subject','evaluated']+[g+s for g in GROUPS for s in ['_alive','_lost']]
    csvout(RESULTS/'survival.csv',curves,fields)
    csvout(RESULTS/'first-failures.csv',losses,['index','sha','group','id','class','method','kind','flaky'])
    write_json(RESULTS/'alive-entering.json',entering)
    evaluated=[r for r in curves if r['evaluated']=='yes'];last=evaluated[-1]
    events=[r for r in curves if int(r['index'])==0 or any(r[g+'_lost'] for g in GROUPS) or r['index']==last['index']]
    lines=['# Table 1. Survival by event','','| Index | Date | Subject | '+' | '.join(LABELS[g] for g in GROUPS)+' |','|---:|---|---|'+'---:|'*len(GROUPS)]
    for r in events:lines.append('| '+ ' | '.join([r['index'],r['date'][:10],md(r['subject']),*[pct(r[g+'_alive'],len(denominators[g])) for g in GROUPS]])+' |')
    (RESULTS/'table1-survival-by-event.md').write_text('\n'.join(lines)+'\n')
    lines=['# Table 2. Breaking commits','','| Index | Group | Methods lost | First-failure kinds |','|---:|---|---:|---|']
    for i in sorted({r['index'] for r in losses}):
        for g in GROUPS:
            subset=[r for r in losses if r['index']==i and r['group']==g]
            if subset:lines.append(f'| {i} | {LABELS[g]} | {len(subset)} | '+md(dict(collections.Counter(r['kind'] for r in subset)))+' |')
    (RESULTS/'table2-breaking-commits.md').write_text('\n'.join(lines)+'\n')
    nonincreasing=all(all(a[g+'_alive']>=b[g+'_alive'] for a,b in zip(curves,curves[1:])) for g in GROUPS)
    actual=len(list((RESULTS/'raw').glob('*.json')));expected=(len(commits)-len(failed))*len(GROUPS)
    consistency=dict(nonincreasing=nonincreasing,index_zero_equals_denominators=all(curves[0][g+'_alive']==len(denominators[g]) for g in GROUPS),raw_records=actual,expected_raw_records=expected,raw_count_matches=actual==expected,missing_records=missing,build_fail_indices=failed)
    write_json(RESULTS/'survival-consistency.json',consistency)
    (RESULTS/'consistency.md').write_text('# Consistency checks\n\n```json\n'+json.dumps(consistency,indent=2)+'\n```\n')
    sys.path.insert(0,str(SHARED/'work/python'))
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    fig,ax=plt.subplots(figsize=(10,5.5),layout='constrained')
    for g,color in zip(GROUPS,['#2563eb','#dc2626','#059669','#9333ea']):
        n=len(denominators[g])
        if n:ax.step([int(r['index']) for r in curves],[100*r[g+'_alive']/n for r in curves],where='post',label=f'{LABELS[g]} (n={n})',color=color,lw=2)
        else:ax.plot([],[],color=color,label=f'{LABELS[g]} (n=0; unavailable)')
    ax.set(xlabel='First-parent commit index',ylabel='Methods surviving (%)',ylim=(-2,102),xlim=(0,len(commits)),title='Frozen-test survival after 1.1.0')
    ax.grid(alpha=.2);ax.legend(loc='best')
    for suffix in ['png','svg']:fig.savefig(RESULTS/f'figure1-survival-by-commit.{suffix}',dpi=180)
    svg=RESULTS/'figure1-survival-by-commit.svg'
    svg.write_text('\n'.join(line.rstrip() for line in svg.read_text().splitlines())+'\n')
    plt.close(fig)
    print(json.dumps(dict(final={g:last[g+'_alive'] for g in GROUPS},denominators={g:len(rows) for g,rows in denominators.items()},**consistency)),flush=True)


if __name__=='__main__':main()
