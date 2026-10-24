"""Deterministic, import-only repair: initial javac plus at most two repair passes."""
import collections
import json
import re
import time
import zipfile
from adjusted_prompts import BASE
from run_commit import classpath
from start import EXP, execute, write_json

PACKAGES={'org.junit','org.junit.jupiter.api','org.mockito','org.mockito.stubbing','org.mockito.invocation','org.hamcrest'}

def main():
    started=time.monotonic();assert not (BASE/'import-fixes.json').exists(), 'Repair already recorded'
    types=json.loads((BASE/'prompts/type-inventory.json').read_text())
    candidates=collections.defaultdict(set)
    for fqn in types:candidates[fqn.rsplit('.',1)[-1]].add(fqn)
    folder,cp=classpath(0)
    for item in cp.split(':'):
        if not item.endswith('.jar'):continue
        with zipfile.ZipFile(item) as jar:
            for path in jar.namelist():
                if not path.endswith('.class') or '/' not in path:continue
                package,name=path[:-6].rsplit('/',1)
                if package.replace('/','.') not in PACKAGES or re.search(r'\$\d',name):continue
                fqn=(package+'.'+name).replace('/','.').replace('$','.')
                candidates[name.rsplit('$',1)[-1]].add(fqn)
    write_json(BASE/'prompts/import-type-candidates.json',{k:sorted(v) for k,v in sorted(candidates.items())})
    records=[]
    for number,source in enumerate(sorted((BASE/'sources').rglob('*.java'))):
        original=source.read_text();before=source.with_suffix('.java.pre-repair');assert not before.exists();before.write_text(original)
        classes=EXP/f'work/import-repair/ouedraogo-adj/{number}/classes';classes.mkdir(parents=True,exist_ok=True)
        record=dict(file=str(source.relative_to(EXP)),pre_repair=str(before.relative_to(EXP)),added_imports=[],passes=[])
        for attempt in range(3):
            log=BASE/f'import-repair/{number}/compile-{attempt}.txt'
            result=execute(['javac','-J-Xmx512m','-Xmaxerrs','1000','-cp',cp,'-sourcepath',str(source.parent),'-d',str(classes),str(source)],folder,log,300)
            diagnostic=log.read_text(errors='replace')
            symbols=sorted(set(re.findall(r'cannot find symbol\n(?:[^\n]*\n)*?\s*symbol:\s+class\s+(\w+)',diagnostic)))
            fixes=[];ambiguous={}
            if result['exit_code'] and attempt<2:
                for symbol in symbols:
                    choices=sorted(candidates.get(symbol,[]))
                    if len(choices)==1 and choices[0] not in record['added_imports']:fixes.append(choices[0])
                    elif len(choices)>1:ambiguous[symbol]=choices
            record['passes'].append(dict(compile=result,missing_classes=symbols,added_imports=fixes,ambiguous=ambiguous))
            if not fixes:break
            text=source.read_text();imports=''.join('import '+fqn+';\n' for fqn in fixes)
            package=re.search(r'^\s*package\s+[^;]+;',text,re.M)
            position=package.end() if package else 0
            source.write_text(text[:position]+'\n'+imports+text[position:])
            record['added_imports'].extend(fixes)
        repaired=source.read_text()
        # Removing exactly the inserted import lines must recover the original text
        # modulo blank lines inserted around the package declaration.
        stripped=repaired
        for fqn in record['added_imports']:stripped=stripped.replace('import '+fqn+';\n','',1)
        assert ''.join(stripped.split())==''.join(original.split()), source
        record['compiled_after_repair']=record['passes'][-1]['compile']['exit_code']==0
        records.append(record);write_json(BASE/'import-fixes.json',dict(files=records,seconds=round(time.monotonic()-started,3),max_repair_passes=2))
        print(json.dumps(dict(file=source.name,imports=len(record['added_imports']),compiled=record['compiled_after_repair'])),flush=True)
    if not records:write_json(BASE/'import-fixes.json',dict(files=[],seconds=round(time.monotonic()-started,3),max_repair_passes=2))

if __name__=='__main__':main()
