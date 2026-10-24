"""Pre-call gate: source-faithful headers/varargs; only permitted block differences."""
import datetime
import difflib
import hashlib
import json
import re
from start import EXP,REPO,execute,write_json

BASE=EXP/'generated/ouedraogo-adj2/round1'
OLD=EXP/'generated/ouedraogo-adj/round1'
MODIFIERS=r'public|protected|private|abstract|static|final|strictfp|sealed|non-sealed'

def regression():
    log=BASE/'prompts/renderer-regression.tsv'
    run=execute(['java',str(EXP/'tools/DependencyInventory2.java'),str(EXP/'tools/fixtures/renderer')],REPO,log,300)
    assert run['exit_code']==0,run
    rows=[line.split('\t') for line in log.read_text().splitlines() if line.startswith(('T\t','A\t'))]
    types={r[1]:r for r in rows if r[0]=='T'}
    api=[r[2] for r in rows if r[0]=='A']
    assert types['renderer.fixture.Generic.Contract.Nested'][4]=='public static'
    assert types['renderer.fixture.Generic'][4:6]==['public abstract','T extends Number']
    assert types['renderer.fixture.Generic.Plain'][4:6]==['public static final','']
    assert types['renderer.fixture.Generic.Mode'][3:5]==['ENUM','public']
    assert types['renderer.fixture.Generic.Contract'][3:5]==['INTERFACE','public']
    for signature in ['protected Generic(String... names);','public abstract void accept(T... values);','public void arrays(String[] values);']:
        assert signature in api,(signature,api)
    assert any('run(String... values)' in s for s in api)
    result=dict(passed=True,generic_parameters=True,empty_parameters_omitted=True,declaration_modifiers=True,varargs_preserved=True,ordinary_arrays_preserved=True,run=run)
    write_json(BASE/'prompts/renderer-regression.json',result)
    return result

def check(write=False):
    if write:
        assert not (BASE/'runs').exists(),'Never rewrite a sent prompt or gate record'
        regression()
    else:assert json.loads((BASE/'prompts/renderer-regression.json').read_text())['passed']
    types=json.loads((BASE/'prompts/type-inventory.json').read_text())
    scope=json.loads((EXP/'population/scope.json').read_text())['classes']
    details=[];diff=['# Corrected dependency-block diff','','Compared with the frozen addendum 2 blocks. Only declaration headers and varargs notation change; dependency order, membership, omissions, API order, constraints, and caps are unchanged.','']
    for cls in scope:
        name=cls['class'].rsplit('.',1)[-1]
        old=(OLD/'prompts'/name/'dependency-signatures.txt').read_text()
        new=(BASE/'prompts'/name/'dependency-signatures.txt').read_text()
        prior=json.loads((OLD/'prompts'/name/'dependency-manifest.json').read_text())
        current=json.loads((BASE/'prompts'/name/'dependency-manifest.json').read_text())
        assert prior==current,(name,'dependency order/cap metadata changed')
        assert not re.search(r'<\s*>',new),(name,'empty type parameters')
        oldlines=old.splitlines();newlines=new.splitlines();assert len(oldlines)==len(newlines)<=400
        headers=[];varargs=0;fqn=None
        for a,b in zip(oldlines,newlines):
            if b.startswith('// public API of '):fqn=b.removeprefix('// public API of ')
            if re.match(r'^(class|interface|enum) ',a):
                assert re.match(r'^(?:(?:'+MODIFIERS+r')\s+)+(?:class|interface|enum)\s+',b),(name,b)
                t=types[fqn]
                expected=t['modifiers']+' '+t['kind'].lower()+' '+fqn.rsplit('.',1)[-1]+('<'+t['parameters']+'>' if t['parameters'] else '')
                assert b.startswith(expected+' ') or b==expected+' {',(fqn,b,expected)
                normalized=re.sub(r'^(?:(?:'+MODIFIERS+r')\s+)+','',b)
                assert normalized==a.replace('<>',''),(name,a,b)
                headers.append(b)
            else:
                assert b.replace('...','[]')==a,(name,'unrelated block edit',a,b)
                varargs+=b.count('...')
        assert len(headers)==len(current['included'])<=15
        before=(OLD/'prompts'/name/'appended-block.txt').read_text()
        after=(BASE/'prompts'/name/'appended-block.txt').read_text()
        assert before.removesuffix(old)==after.removesuffix(new),'Constraint changed'
        for tech in ['ZSL','FSL','CoT','ToT','GToT']:
            prior_prompt=(OLD/'prompts/rendered'/name/(tech+'.txt')).read_text()
            new_prompt=(BASE/'prompts/rendered'/name/(tech+'.txt')).read_text()
            assert prior_prompt.replace(before,after,1)==new_prompt
        diff+=['## '+name,'','```diff',*list(difflib.unified_diff(oldlines,newlines,fromfile='ouedraogo-adj/'+name,tofile='ouedraogo-adj2/'+name,lineterm='')),'```','']
        details.append(dict(class_name=name,types=len(headers),lines=len(newlines),varargs_parameters=varargs,headers=headers,old_sha256=hashlib.sha256(old.encode()).hexdigest(),new_sha256=hashlib.sha256(new.encode()).hexdigest()))
    content='\n'.join(diff)+'\n'
    code={name:hashlib.sha256((EXP/'tools'/name).read_bytes()).hexdigest() for name in ['DependencyInventory2.java','adjusted2_prompts.py','check_adjusted2_blocks.py']}
    if write:
        (BASE/'prompts/block-diff.md').write_text(content)
        write_json(BASE/'prompts/pre-call-check.json',dict(passed=True,checked_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),empty_angle_brackets=False,headers_have_modifiers=True,only_requested_changes=True,renderer_sha256=code,blocks=details))
    else:
        assert (BASE/'prompts/block-diff.md').read_text()==content
        saved=json.loads((BASE/'prompts/pre-call-check.json').read_text())
        assert saved['passed'] and saved['blocks']==details and saved['renderer_sha256']==code
    print(json.dumps(dict(pre_call_check='passed',blocks=len(details),types=sum(d['types'] for d in details),varargs_parameters=sum(d['varargs_parameters'] for d in details))),flush=True)

if __name__=='__main__':check()
