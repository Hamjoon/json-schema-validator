"""Freeze ordered, capped dependency signatures and append the exact constraints."""
import collections
import hashlib
import json
import re
from start import EXP, REPO, RESULTS, execute, write_json

BASE=EXP/'generated/ouedraogo-adj2/round1'
CONSTRAINT='Use only classes, constructors and methods that appear in the class source above or in the dependency signatures below. Do not use APIs from other versions of this library. Construct real objects through the constructors or factory methods shown instead of mocking; do not mock or subclass abstract classes or interfaces of this library.'
TECHNIQUES=['ZSL','FSL','CoT','ToT','GToT']

def main():
    assert not (BASE/'runs').exists(), 'Prompts are immutable after generation starts'
    log=BASE/'prompts/source-api-inventory.tsv'
    result=execute(['java',str(EXP/'tools/DependencyInventory2.java'),str(REPO/'src/main/java')],REPO,log,300)
    assert result['exit_code']==0,result
    types={};imports=collections.defaultdict(list);refs=collections.defaultdict(lambda:collections.defaultdict(list));apis=collections.defaultdict(list)
    for line in log.read_text().splitlines():
        p=line.split('\t')
        if len(p)<2:continue
        kind,fqn=p[:2]
        if kind=='T':types[fqn]=dict(package=p[2],kind=p[3],modifiers=p[4],parameters=p[5],extends=p[6],implements=p[7])
        elif kind=='I':imports[fqn].append(p[2])
        elif kind=='R':refs[fqn][p[2]].append(p[3])
        elif kind=='A':apis[fqn].append(p[2])
    write_json(BASE/'prompts/type-inventory.json',types)
    def resolve(owner,expr):
        found=[]
        for token in re.findall(r'[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*',expr):
            candidates=[]
            if token in types:candidates.append(token)
            for imp in imports[owner]:
                if imp.endswith('.*'):
                    cand=imp[:-1]+token
                else:
                    first,*tail=token.split('.')
                    cand=imp+('.'+'.'.join(tail) if tail else '') if imp.rsplit('.',1)[-1]==first else ''
                if cand in types:candidates.append(cand)
            for prefix in [owner,types[owner]['package']]:
                if prefix+'.'+token in types:candidates.append(prefix+'.'+token)
            candidates=list(dict.fromkeys(candidates))
            if len(candidates)==1 and candidates[0] not in found:found.append(candidates[0])
        return found
    scope=json.loads((EXP/'population/scope.json').read_text())['classes'];manifest=[]
    for cls in scope:
        fqn=cls['class'];name=fqn.rsplit('.',1)[-1];ordered=[];reasons={}
        def add(t,why):
            if t!=fqn and t not in ordered:ordered.append(t);reasons[t]=why
        visited=set()
        def supers(t):
            if t in visited:return
            visited.add(t)
            for expr in refs[t]['super']:
                for dep in resolve(t,expr):add(dep,'superclass/interface chain');supers(dep)
        supers(fqn)
        for category in ['constructor','field','method']:
            for expr in refs[fqn][category]:
                for dep in resolve(fqn,expr):add(dep,category)
        # Explicit project imports, including nested types; expand wildcard imports
        # only to types actually referenced in this source.
        source=(REPO/cls['file']).read_text()
        for imp in imports[fqn]:
            if not imp.startswith('com.networknt.schema'):continue
            if imp in types:add(imp,'import')
            elif imp.endswith('.*'):
                for token in dict.fromkeys(re.findall(r'\b[A-Za-z_$][\w$]*\b',source)):
                    if imp[:-1]+token in types:add(imp[:-1]+token,'wildcard import reference')
        # Include project types referenced by nested builders' declarations as well.
        for nested in types:
            if nested.startswith(fqn+'.'):
                add(nested,'nested declaration')
                for category in ['super','constructor','field','method']:
                    for expr in refs[nested][category]:
                        for dep in resolve(nested,expr):add(dep,'nested '+category)
        blocks=[];kept=[];omitted=[];lines=0
        for dep in ordered:
            t=types[dep];simple=dep.rsplit('.',1)[-1]
            params='<'+t['parameters']+'>' if t['parameters'] else ''
            header=(t['modifiers']+' '+t['kind'].lower()+' '+simple+params).strip()
            if t['extends']:header+=' extends '+t['extends']
            if t['implements']:header+=(' extends ' if t['kind']=='INTERFACE' else ' implements ')+t['implements']
            block=['// public API of '+dep,header+' {',*['  '+a for a in apis[dep]],'}','']
            if omitted or len(kept)>=15 or lines+len(block)>400:omitted.append(dep);continue
            kept.append(dep);blocks.extend(block);lines+=len(block)
        block='\n'.join(blocks)+'\n'
        dest=BASE/'prompts'/name;dest.mkdir(parents=True,exist_ok=True)
        (dest/'dependency-signatures.txt').write_text(block)
        appended='\n\n'+CONSTRAINT+'\n\n'+block
        (dest/'appended-block.txt').write_text(appended)
        metadata=dict(class_name=fqn,ordered_types=ordered,included=kept,omitted=omitted,reasons=reasons,types=len(kept),lines=len(block.splitlines()),limits=dict(types=15,lines=400))
        write_json(dest/'dependency-manifest.json',metadata)
        # Insert immediately after the class source, preserving all author template text.
        for tech in TECHNIQUES:
            original=EXP/'generated/ouedraogo/round1/prompts/rendered'/name/(tech+'.txt')
            prompt=original.read_text();assert prompt.count(source)==1
            prompt=prompt.replace(source,source+appended,1)
            path=BASE/'prompts/rendered'/name/(tech+'.txt');path.parent.mkdir(parents=True,exist_ok=True);path.write_text(prompt)
            manifest.append(dict(class_name=name,technique=tech,sha256=hashlib.sha256(prompt.encode()).hexdigest()))
    write_json(BASE/'prompts/manifest.json',manifest)
    from check_adjusted2_blocks import check
    check(write=True)

if __name__=='__main__':main()
