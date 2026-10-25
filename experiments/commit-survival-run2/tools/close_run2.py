"""Report run 2 from measured artifacts, including timings and deviations."""
import csv
import datetime
import json
from start import EXP, RESULTS, write_json


def read(name):return json.loads((RESULTS/name).read_text())


def main():
    anchor=read('anchor.json');scope=json.loads((EXP/'population/scope.json').read_text())
    denom=read('denominators.json');survival=read('survival-consistency.json');coverage=read('coverage-consistency.json')
    verification=read('final-verification.json');notes=read('execution-notes.json')
    models={p.stem:json.loads(p.read_text()) for pattern in ['ktester-round*.json','ouedraogo-round*.json'] for p in RESULTS.glob(pattern) if 'compile-failures' not in p.name}
    timings={k:read(k+'.json')['seconds'] for k in ['step1','phase-a','phase-b']}
    timings['final_anchor_control']=sum(r['seconds'] for r in read('control-at-t.json').values())
    timings.update({k:v['seconds'] for k,v in models.items()})
    for p in (EXP/'generated').glob('*/*/funnel.json'):
        f=json.loads(p.read_text());timings[f['group']+'-'+f['round']+'-screening']=f['seconds']
    curves=list(csv.DictReader((RESULTS/'survival.csv').open()))
    final={g:int(curves[-1][g+'_alive']) for g in denom}
    now=datetime.datetime.now(datetime.timezone.utc)
    summary=dict(status='completed' if not survival['missing_records'] and not coverage['missing'] else 'completed-with-unavailable-measurements',
                 finished=now.isoformat(),wall_seconds=round((now-datetime.datetime.fromisoformat(read('addendum-start.json')['started'])).total_seconds(),3),
                 phase_seconds=timings,timing_note='Wall time starts at addendum resumption. Prior stopped Step 1 is preserved separately. Anchor builds, index, original baseline, commit builds and changed-line enumeration are reused and contribute no new execution time.',
                 scope=scope,denominators=denom,ktester_ratio_to_dev=denom['ktester']/denom['dev'],final_survivors=final,model_rounds=models,
                 model_call_counts={g:sum(v['model_calls'] for k,v in models.items() if k.startswith(g+'-')) for g in ['ktester','ouedraogo']},
                 phases={k:read(k+'.json') for k in ['phase-a','phase-b']},deviations=notes['deviations'],clarifications=notes['clarifications'],
                 survival_consistency=survival,coverage_consistency=coverage,final_verification=verification,
                 artifact_audit=read('run2-artifact-audit.json'),pushed=False)
    write_json(RESULTS/'final-summary.json',summary)
    caveats='\n'.join('- '+k+': '+v['repair_error']+'; stopped at target '+str(v.get('repair_stopped_at_target'))+'.' for k,v in models.items() if v.get('repair_error'))
    empty_groups=[g for g,n in denom.items() if not n]
    if empty_groups:
        caveats+='\n\nNo methods survived generation screening for '+', '.join(empty_groups)+'. Their survival percentages are n/a; no survival curve can be estimated. Coverage from an empty retained suite is zero and does not establish comparative test effectiveness. See every original/retry funnel and compiler diagnostic report.'
    (RESULTS/'STATUS.md').write_text('# Run 2 status\n\n'+summary['status']+'.\n\n'+f"Denominators: Dev {denom['dev']}, KTester {denom['ktester']}, Ouédraogo {denom['ouedraogo']}. Final survivors: {final}. All 229 commit indices and 181 required coverage indices processed.\n\n"+caveats+'\n\nSee final-summary.json for model calls, phase timings, worker/quarantine records and verification. No author code patched. No push performed.\n')
    (EXP/'README.md').write_text(f'''# Commit-survival experiment, run 2

Report: [Commit-survival study](docs/jsv-commit-survival-report.md).

The KTester tool run is recorded in `Hamjoon/KTester`, branch `experiment/2026-10-week4-jsv-commit-survival`, folder `data/commit-survival/json-schema-validator/`.

This run compares project-wide frozen developer tests with independently generated KTester and Ouédraogo tests at tag `1.1.0`, across 229 first-parent commits. The developer denominator is all 185 methods stable in run 1. Abstract production classes remain eligible; classes dominated by trivial accessors are excluded.

Denominators: **Dev {denom['dev']}, KTester {denom['ktester']}, Ouédraogo {denom['ouedraogo']}**. KTester/Dev ratio: {denom['ktester']/185:.4f}. Final survivors: **Dev {final['dev']}, KTester {final['ktester']}, Ouédraogo {final['ouedraogo']}**.

Scope: {', '.join('`'+r['class'].rsplit('.',1)[-1]+'`' for r in scope['classes'])}; {scope['m']} focal methods. [Ranked scope](population/scope.md) and [accessor audit](population/accessor-audit.csv) explain why the executable rule yields 64 methods rather than the illustrative 73-method expectation. The addendum removes the minimum class count. The superseded stopped state is retained under `results/stopped-step1/`.

Anchor: `{anchor['sha']}`. Range endpoint: `{anchor['origin_master']}`. Build failures are excluded and carry survival forward. [Reuse provenance](results/reuse.md) and `reuse-manifest.json` document the copied run 1 inputs. Checkouts, classpath caches and runtime libraries are reused in place.

Results: [survival events](results/table1-survival-by-event.md), [breaking commits](results/table2-breaking-commits.md), [changed-line coverage](results/table3-changed-line-coverage.md), [exposure versus break](results/table4-exposure-vs-break.md), [anchor coverage](results/coverage-t.md), [figure](results/figure1-survival-by-commit.png), [consistency](results/consistency.md), [final verification](results/final-verification.json). `results/final-summary.json` contains timings, model-call counts, failures, quarantine events and final counts.

{caveats or 'No recorded author post-processing interruption.'}

`dev/` holds the full frozen Java sources and fixtures; `population/` contains the audited scope and exact dataset; `generated/` preserves prompts, responses, generated files and independent three-run screening evidence. Per-prompt funnels are beside each round. `results/ouedraogo-round1-compile-failures.json` categorizes compiler failures without modifying prompts or generated sources. `tools/ORIGIN.md` records copied and adapted tooling.

Every Dev measurement attempts compilation of the whole frozen Java suite. If that fails, class-isolated compilation attributes failure only to affected classes, preserving run 1's first-failure rule. Successful whole-suite compilation is shared by separate class launches; each class retains its 120-second timeout. All 185 denominator methods are evaluated in Phase A. Phase B runs only methods alive entering each index. Fail/error outcomes receive one rerun; a passing rerun remains alive and is marked flaky. Cumulative survival uses fixed denominators. Compile failures apply to every denominator method in that class. Unavailable measurements are never converted to zero.

Changed-line coverage uses post-image executable lines, split all/scope/other files. Exposure uses pre-image lines at c−1. Scope paths stay frozen to the anchor. The consistency checks compare executable line sets across all three groups.

## Reproduction

All Java builds/tests and model calls execute in the KTester container. Copy run 1 inputs as listed in `results/reuse.md`, retaining run 1 caches. Use the existing compose configuration and credential environment file; never commit credentials. From KTester/docker:

```sh
run() {{ docker compose -f compose.yml -f compose.commit-survival.yml run --rm ktester "$@"; }}
T=/repo/experiments/commit-survival-run2/tools
run bash -c 'java /repo/experiments/commit-survival-run2/tools/AccessorAudit.java /repo/src/main/java > /repo/experiments/commit-survival-run2/population/accessor-methods.tsv'
run python "$T/select_scope_run2.py"
run python "$T/prepare_ktester.py" dataset
run python "$T/generate_ktester.py" --round 1
run python "$T/validate_round.py" --group ktester --round round1
run python "$T/describe_round.py" ktester round1
run python "$T/generate_ouedraogo.py" --round 1
run python "$T/validate_round.py" --group ouedraogo --round round1
run python "$T/describe_round.py" ouedraogo round1
# Repeat Ouédraogo rounds while kept < 185, at most 3 rounds.
# Retry a zero-compiled round once after 60 seconds using --attempt 2
# and screen its roundN-retry label. KTester has one round only.
run python "$T/control_at_t.py"
run python "$T/run_phase.py" --phase a --workers 4
run python "$T/aggregate_survival.py"
# Reuse the copied changes/ and coverage-commits.csv; do not recompute.
run python "$T/run_phase.py" --phase b --workers 4
run python "$T/aggregate_coverage.py"
run python "$T/final_check.py"
run python "$T/audit_run2.py"
run python "$T/close_run2.py"
```

Generation refuses to overwrite existing call directories. Phase records resume only with matching source and runner hashes. OOM/exit −9 records are quarantined and resumed at two workers. The host driver `tools/continue_run2.py` records stage events and local step commits. Author code and prompt templates remain unchanged. No push.
''')
    print(json.dumps(dict(status=summary['status'],denominators=denom,final_survivors=final,model_call_counts=summary['model_call_counts'])),flush=True)


if __name__=='__main__':main()
