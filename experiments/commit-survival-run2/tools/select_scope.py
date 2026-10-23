"""Map reported cases to source methods and apply the prescribed scope rule."""
import collections
import csv
import json
import re
import xml.etree.ElementTree as ET
from start import EXP, RESULTS, write_json


def csvout(path, rows, fields):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('w',newline='') as stream:
        w=csv.DictWriter(stream,fieldnames=fields,extrasaction='ignore',lineterminator='\n');w.writeheader();w.writerows(rows)


def main():
    inventory=json.loads((RESULTS/'source-inventory.json').read_text())
    tests=[r for r in inventory['test']['methods'] if r['kind']]
    byclass=collections.defaultdict(list)
    for r in tests:byclass[r['class']].append(r)
    observations=[];mapping=[];unmapped=[]
    for run in range(1,4):
        observed=collections.defaultdict(list)
        for path in sorted((RESULTS/f'baseline-reports/run{run}').glob('TEST-*.xml')):
            cls=path.name[5:-4]
            for case in ET.parse(path).getroot().iter('testcase'):
                name=case.get('name','')
                candidates=[m for m in byclass[cls] if re.match(re.escape(m['method'])+r'(?:\(|\[|$)',name)]
                if not candidates:
                    candidates=[m for m in byclass[cls] if m['display'] and m['display'] in name]
                    if candidates:
                        longest=max(len(m['display']) for m in candidates)
                        candidates=[m for m in candidates if len(m['display'])==longest]
                if len(candidates)!=1:
                    unmapped.append(dict(run=run,**{'class':cls},name=name,candidates=[m['method'] for m in candidates]));continue
                method=candidates[0]['method']
                state='error' if case.find('error') is not None else 'fail' if case.find('failure') is not None else 'skipped' if case.find('skipped') is not None else 'pass'
                observed[(cls,method)].append(state)
                mapping.append(dict(run=run,**{'class':cls},method=method,raw_name=name,state=state))
        observations.append(observed)
    write_json(RESULTS/'baseline-unmapped.json',unmapped)
    assert not unmapped, f'{len(unmapped)} cases need unambiguous source mapping'
    rows=[]
    for method in tests:
        row={k:method[k] for k in ['class','method','kind']}
        for i,obs in enumerate(observations,1):
            states=obs.get((row['class'],row['method']),[])
            row[f'run{i}']='pass' if states and set(states)=={'pass'} else ','.join(sorted(set(states))) if states else 'absent'
        row['stable']='yes' if all(row[f'run{i}']=='pass' for i in range(1,4)) else 'no'
        rows.append(row)
    csvout(EXP/'dev/dev-methods-all.csv',rows,['class','method','kind','stable','run1','run2','run3'])
    csvout(EXP/'dev/surefire-method-map.csv',mapping,['run','class','method','raw_name','state'])
    changes=json.loads((RESULTS/'commit-paths.json').read_text())
    eligible=collections.defaultdict(list)
    for method in inventory['main']['methods']:
        if method['eligible']:eligible[method['class']].append(method)
    survey=[]
    for cls,methods in eligible.items():
        data=inventory['main']['classes'][cls]
        test=cls+'Test';has_test=test in inventory['test']['classes'] and inventory['test']['classes'][test]['path']=='src/test/java/'+test.replace('.','/')+'.java'
        survey.append(dict(**{'class':cls},file=data['path'],m=len(methods),has_test='yes' if has_test else 'no',d=sum(r['class']==test and r['stable']=='yes' for r in rows),c=sum(data['path'] in paths for paths in changes.values())))
    survey.sort(key=lambda r:(-r['c'],-r['d'],r['class']))
    csvout(EXP/'population/class-survey.csv',survey,['class','file','m','has_test','d','c'])
    table='| Class | m | has_test | d | c |\n|---|---:|---|---:|---:|\n'+'\n'.join(f"| {r['class']} | {r['m']} | {r['has_test']} | {r['d']} | {r['c']} |" for r in survey)+'\n'
    (EXP/'population/class-survey.md').write_text('# Production class survey\n\nDeclared top-level class methods only; constructors, private, deprecated, and bodyless declarations excluded.\n\n'+table)
    chosen=[]
    for row in survey:
        if row['has_test']!='yes' or row['m']<3:continue
        chosen.append(row)
        if len(chosen)>=3 and (sum(r['m'] for r in chosen)>=50 or sum(r['d'] for r in chosen)>=150 or len(chosen)==10):break
    scope=dict(classes=chosen,m=sum(r['m'] for r in chosen),d=sum(r['d'] for r in chosen))
    write_json(EXP/'population/scope.json',scope)
    (EXP/'population/scope.md').write_text('# Selected scope\n\n'+f"Selected {len(chosen)} classes, cumulative m={scope['m']}, d={scope['d']}. Sorted by c descending, d descending, then fully-qualified name. All eligible candidates are used if neither threshold is reached.\n\n"+'\n'.join(f"- {r['class']}: m={r['m']}, d={r['d']}, c={r['c']}." for r in chosen)+'\n')
    production_names={c.rsplit('.',1)[-1] for c in inventory['main']['classes'] if inventory['main']['classes'][c]['top']}
    def scoped(cls):
        pkg,_,simple=cls.rpartition('.')
        for c in chosen:
            cp,_,name=c['class'].rpartition('.')
            if pkg!=cp or not simple.startswith(name) or not simple.endswith('Test'):continue
            suffix=simple[len(name):-4]
            if not suffix or suffix not in production_names:return True
        return False
    denominator=[r for r in rows if r['stable']=='yes' and scoped(r['class'])]
    csvout(EXP/'dev/dev-denominator.csv',denominator,['class','method','kind'])
    csvout(EXP/'population/methods.csv',[m for c in chosen for m in eligible[c['class']]],['class','method','signature','line'])
    write_json(RESULTS/'scope-selection.json',dict(classes=len(chosen),m=scope['m'],d=scope['d'],developer_denominator=len(denominator),all_test_methods=len(rows),stable_test_methods=sum(r['stable']=='yes' for r in rows),mapping_cases=len(mapping)))
    print((RESULTS/'scope-selection.json').read_text(),flush=True)
    if len(chosen)<3:
        (RESULTS/'STATUS.md').write_text('# Experiment status\n\nSTOP: Step 2 selected fewer than three classes.\n')
        raise SystemExit(2)


if __name__=='__main__':main()
