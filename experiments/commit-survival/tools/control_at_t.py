"""Final control run for all three frozen denominators, before the commit loop."""
import collections
import json
from start import RESULTS, write_json
from run_commit import GROUPS, denominator, run_group


def main():
    rows={g:denominator(g) for g in GROUPS}
    write_json(RESULTS/'frozen-denominators.json',rows)
    results={}
    for g in GROUPS:
        result=run_group(0,g,'control-final')
        results[g]=dict(denominator=len(rows[g]),states=dict(collections.Counter(r['state'] for r in result['methods'])),seconds=result['seconds'],passed=all(r['state']=='pass' for r in result['methods']))
    write_json(RESULTS/'control-at-t.json',results)
    (RESULTS/'control-at-t.md').write_text('# Final anchor control\n\n```json\n'+json.dumps(results,indent=2)+'\n```\n')
    print(json.dumps(results),flush=True)
    assert all(r['passed'] for r in results.values()),'Final denominator control failed'
    assert len(rows['ktester'])+len(rows['ouedraogo'])>0,'Mandatory stop: both generated groups are empty'


if __name__=='__main__':main()
