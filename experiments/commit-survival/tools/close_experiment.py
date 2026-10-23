"""Write final status, reproducibility instructions and local handover from records."""
import csv
import datetime
import json
from start import EXP, RESULTS, write_json


def main():
    anchor=json.loads((RESULTS/'anchor.json').read_text())
    scope=json.loads((EXP/'population/scope.json').read_text())
    denominators=json.loads((RESULTS/'denominators.json').read_text())
    survival=json.loads((RESULTS/'survival-consistency.json').read_text())
    coverage=json.loads((RESULTS/'coverage-consistency.json').read_text()) if (RESULTS/'coverage-consistency.json').exists() else None
    notes=json.loads((RESULTS/'execution-notes.json').read_text())
    phases={name:json.loads((RESULTS/(name+'.json')).read_text()) for name in ['build-check','developer-baseline','phase-a','phase-b'] if (RESULTS/(name+'.json')).exists()}
    model={p.stem:json.loads(p.read_text()) for pattern in ['ktester-round*.json','ouedraogo-round[123].json'] for p in RESULTS.glob(pattern) if 'compile-failures' not in p.name}
    now=datetime.datetime.now(datetime.timezone.utc)
    result=dict(status='completed' if not survival['missing_records'] and coverage is not None and not coverage['missing'] else 'completed-with-unavailable-measurements',finished=now.isoformat(),wall_seconds=round((now-datetime.datetime.fromisoformat(anchor['started'])).total_seconds(),3),phases=phases,model_rounds=model,denominators=denominators,deviations=notes['deviations'],survival_consistency=survival,coverage_consistency=coverage)
    write_json(RESULTS/'final-summary.json',result)
    (RESULTS/'STATUS.md').write_text('# Experiment status\n\n'+result['status']+'.\n\n'+f"Anchor: `{anchor['sha']}`. Complete prescribed range: {anchor['n']} first-parent commits ending at `{anchor['origin_master']}`. Denominators: Dev {denominators['dev']}, KTester {denominators['ktester']}, five-prompt {denominators['ouedraogo']}.\n\n"+'See final-summary.json for phase timings, model-call counts, recorded setup failures and deviations. No author code patched. No push performed.\n')
    handovers='\n\n'.join(p.read_text() for p in sorted((EXP/'docs').glob('handover-part*.md')))
    (EXP/'docs/handover-final.md').write_text('# Final handover\n\n'+handovers+'\n\n## Recorded final state\n\n```json\n'+json.dumps(result,indent=2)+'\n```\n')
    readme=f'''# Commit-survival experiment

This experiment compares frozen developer tests with independently generated method-focused and five-prompt test groups. Tests are screened at tag `1.1.0` and evaluated cumulatively against the complete first-parent range to the pinned fork master.

Anchor: `{anchor['sha']}`. Range endpoint: `{anchor['origin_master']}`. Range length: {anchor['n']} commits. Build-fail indices are excluded from evaluation and carry survival forward.

Scope: {', '.join('`'+r['class']+'`' for r in scope['classes'])}. These are all eligible classes with a dedicated test file and at least three eligible declared methods. Cumulative focal methods: {scope['m']}. Developer denominator: {denominators['dev']}; method-focused denominator: {denominators['ktester']}; five-prompt denominator: {denominators['ouedraogo']}.

`results/` contains the anchor, full commit/build ledger, baseline reports, raw survival records, JaCoCo XML and line CSVs, four tables, figure, consistency checks, and final status. `dev/` contains frozen Java/resources/suite inputs and the developer method denominator. `population/` contains the source inventory selection and exact KTester dataset. `generated/` retains prompts, responses, original test sources, and validation funnels. `tools/ORIGIN.md` and `tools/origin.json` record copied-tool provenance and local adaptations. Ignored `work/` and `checkouts/` contain reproducible build/runtime intermediates; ignored `docs/handover-*.md` contain local handovers.

## Reproduction

All builds, tests, and model calls run in the KTester JDK 17 container. Start from the required branch bases and configure `docker/compose.commit-survival.yml` with the JSV mount at `/repo`, read-only source mounts at `/jsoup` and `/codec`, and `env_file: [.env]`. Keep the environment file untracked. Commands below run from KTester/docker; the experiment must start without prior model-round directories. Resume build/phase commands from their complete JSON records. Generation scripts refuse to overwrite completed rounds.

```sh
run() {{ docker compose -f compose.yml -f compose.commit-survival.yml run --rm ktester "$@"; }}
T=/repo/experiments/commit-survival/tools
run python "$T/start.py"
run python "$T/baseline.py"
run python "$T/check_commits.py"
run python "$T/inventory.py"
run python "$T/select_scope.py"
run python "$T/prepare_ktester.py" prepare
run python "$T/prepare_ktester.py" index
run python "$T/prepare_ktester.py" dataset
run python "$T/prepare_runner.py"
run python "$T/selfcheck.py"
run python -m pip install --target /repo/experiments/commit-survival/work/python requests==2.32.3 matplotlib==3.10.7
run python "$T/generate_ktester.py" --round 1
run python "$T/validate_round.py" --group ktester --round round1
run python "$T/generate_ouedraogo.py" --round 1
run python "$T/validate_round.py" --group ouedraogo --round round1
run python "$T/control_at_t.py"
run python "$T/run_phase.py" --phase a --workers 4
run python "$T/aggregate_survival.py"
run python "$T/prepare_coverage.py"
run python "$T/run_phase.py" --phase b --workers 4
run python "$T/aggregate_coverage.py"
run python "$T/close_experiment.py"
```

After each group round, compare its kept denominator to the developer denominator. Run another identical round only if smaller, up to three rounds. Retry a zero-compiled round once after 60 seconds. This run's initial KTester metadata preflight made no calls and its permitted retry is archived as `round1-retry`; validate that label when resuming this run. The final metadata adapter preserves exact index signatures. All setup failures and corrections are retained in `results/execution-notes.json`.

Each generated file is compiled independently and a method must pass three runs. Exact whitespace-stripped body duplicates are excluded from denominators without rewriting archived source. The final anchor control selects every retained method once more. Phase A uses a fixed denominator; a fail/error gets one rerun, while compilation failure affects every denominator method in that file. Phase B selects only methods alive entering an index. Coverage compares post-image added/modified executable lines, split by scope; exposure uses pre-image changed lines at the immediate predecessor. Missing measurements are never converted to zero. The final consistency files list all build failures and missing records.

Commit the completed step artifacts locally with the required attribution trailer. Do not push. Existing unrelated KTester changes must remain outside the experiment commits.
'''
    (EXP/'README.md').write_text(readme)
    print(json.dumps(dict(status=result['status'],wall_seconds=result['wall_seconds'],denominators=denominators)),flush=True)


if __name__=='__main__':main()
