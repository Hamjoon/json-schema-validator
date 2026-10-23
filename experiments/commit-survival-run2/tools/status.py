"""Read-only progress summary; never read credentials or response content."""
import datetime
import json
from pathlib import Path
import re
import time

EXP=Path(__file__).resolve().parents[1]
KT=Path('/work') if Path('/work/code').is_dir() else EXP.parents[2]/'KTester'
result=dict(time=datetime.datetime.now(datetime.timezone.utc).isoformat(),builds=len(list((EXP/'results/builds').glob('*.json'))),survival_records=len(list((EXP/'results/raw').glob('*.json'))),coverage_xml=len(list((EXP/'results/coverage').glob('*.xml'))))
dataset=KT/'data/commit-survival/json-schema-validator'
metadata=dataset/'dataset_info.json'
names=[m['test-class'].split('.')[-1] for m in json.loads(metadata.read_text())['json-schema-validator']['focal-methods']] if metadata.exists() else []
rounds={}
for run in sorted((dataset/'rounds').glob('round*')):
    raw=run/'raw-responses'
    row=dict(completed_calls=len(list(raw.glob('*/call.json'))),pending=[dict(call=p.parent.name,age_seconds=round(time.time()-p.stat().st_mtime)) for p in raw.glob('*/request.json') if not (p.parent/'call.json').exists()])
    if (run/'run.json').exists():row.update(json.loads((run/'run.json').read_text()))
    elif (run/'generation.log').exists():
        lines=(run/'generation.log').read_text().splitlines()
        last=next((line for line in reversed(lines) if 'javac -cp @dependencies.txt' in line),'')
        match=re.search(r'/([^/]+)\.java',last);name=match[1] if match else ''
        row.update(active_target=names.index(name)+1 if name in names else None,total_targets=len(names),target=name)
    rounds[run.name]=row
result['ktester']=rounds
print(json.dumps(result))
