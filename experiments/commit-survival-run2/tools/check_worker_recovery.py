"""Exercise scheduling/quarantine using simulated kills, without JVM/model calls."""
import collections
import csv
import json
from pathlib import Path
import sys
import tempfile
import threading
import time
from start import EXP,RESULTS,write_json
import run_phase


def scenario(persistent):
    with tempfile.TemporaryDirectory(dir=EXP/'work',prefix='worker-recovery-') as directory:
        results=Path(directory);(results/'builds').mkdir()
        with (results/'commits.csv').open('w') as f:
            f.write('index\n'+''.join(f'{i}\n' for i in range(1,11)))
        for i in range(1,11):write_json(results/f'builds/{i}.json',{'build':'ok'})
        calls=collections.Counter();lock=threading.Lock()
        def fake(index,group,phase,selection):
            with lock:calls[index]+=1;attempt=calls[index]
            killed=index==1 and (persistent or attempt==1)
            if not killed:time.sleep(.02)
            record=dict(index=index,group=group,status='evaluated',seconds=0,methods=[],files=[{'compile':{'exit_code':-9 if killed else 0},'runs':[]}])
            write_json(results/f'raw/{index}-{group}.json',record)
            return record
        run_phase.RESULTS=results;run_phase.GROUPS=['dev'];run_phase.run_group=fake
        sys.argv=['run_phase.py','--phase','a','--workers','4']
        run_phase.main()
        record=json.loads((results/'phase-a.json').read_text())
        assert record['resume_workers']==2
        assert calls[1]==2 and all(calls[i]==1 for i in range(2,11))
        assert (results/'quarantine/phase-a/1-dev.json').exists()
        assert len(record['failed'])==(1 if persistent else 0)
        assert len(list((results/'raw').glob('*.json')))==(9 if persistent else 10)
        return dict(persistent_kill=persistent,passed=True,unaffected_jobs_completed=9,quarantined_records=2 if persistent else 1)


if __name__=='__main__':
    result=dict(passed=True,scenarios=[scenario(False),scenario(True)])
    write_json(RESULTS/'worker-recovery-check.json',result)
    print(json.dumps(result),flush=True)
