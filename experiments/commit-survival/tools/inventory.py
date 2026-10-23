"""Parse all source declarations with the JDK compiler API, retaining raw evidence."""
import csv
import json
from start import EXP, REPO, RESULTS, execute, write_json


def inventory():
    result = {}
    for group in ['main','test']:
        log=RESULTS/f'source-inventory-{group}.tsv'
        run=execute(['java','-Xmx1g',str(EXP/'tools/SourceInventory.java'),str(REPO/f'src/{group}/java')], REPO, log, 300)
        assert run['exit_code']==0, run
        classes={}; methods=[]
        for line in log.read_text().splitlines():
            parts=line.split('\t')
            if parts[0]=='C':
                _,cls,kind,top,display,path=parts
                classes[cls]=dict(kind=kind,top=top=='true',display=display,path=path.replace(str(REPO)+'/', ''))
            elif parts[0]=='M':
                _,cls,name,kind,eligible,signature,display,lineno,*extra=parts
                methods.append(dict(**{'class':cls},method=name,kind=kind,eligible=eligible=='true',signature=signature,display=display,line=int(lineno)))
        result[group]=dict(classes=classes,methods=methods)
    write_json(RESULTS/'source-inventory.json',result)
    print(json.dumps({g:dict(classes=len(v['classes']),methods=len(v['methods'])) for g,v in result.items()}))


if __name__=='__main__':
    inventory()
