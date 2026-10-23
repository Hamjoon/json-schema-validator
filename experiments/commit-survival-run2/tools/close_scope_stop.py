"""Verify Step 1 and record the inherited stop without issuing model calls."""
import collections
import csv
import datetime
import hashlib
import json
import subprocess
from pathlib import Path
from start import EXP, RESULTS, SHARED, write_json


def main():
    scope = json.loads((EXP/'population/scope.json').read_text())
    assert len(scope['classes']) == 2 and scope['m'] == 80
    dev = list(csv.DictReader((EXP/'dev/dev-denominator.csv').open()))
    population = list(csv.DictReader((EXP/'population/ktester-population.csv').open()))
    assert len(dev) == 185 and len(population) == 80
    assert len({r['id'] for r in population}) == 80
    assert collections.Counter(r['class'] for r in population) == {r['class']: r['m'] for r in scope['classes']}
    manifest = json.loads((RESULTS/'reuse-manifest.json').read_text())
    adapted = {'tools/start.py', 'tools/run_commit.py', 'tools/ORIGIN.md'}
    for row in manifest['files']:
        assert hashlib.sha256((SHARED/row['path']).read_bytes()).hexdigest() == row['sha256'], row['path']
        if row['path'] not in adapted:
            assert hashlib.sha256((EXP/row['path']).read_bytes()).hexdigest() == row['sha256'], row['path']
    frozen = json.loads((EXP/'dev/frozen-source-sha256.json').read_text())
    for path, digest in frozen.items():
        assert hashlib.sha256((EXP/'dev/src-test-t'/path).read_bytes()).hexdigest() == digest, path
    assert not (EXP/'checkouts').exists()
    assert not (EXP/'work/classpaths').exists()
    assert (SHARED/'checkouts/229/target/classes').is_dir()
    assert (SHARED/'work/classpaths').is_dir()
    assert not list((EXP/'generated').rglob('request.json'))
    assert not Path('/work/data/commit-survival/json-schema-validator/rounds/run2-round1').exists()
    subprocess.run(['git', '-C', '/work', 'diff', '--exit-code', 'HEAD', '--', 'code', 'data/projects', 'data/project_index'], check=True)
    begun = json.loads((RESULTS/'run2-start.json').read_text())
    now = datetime.datetime.now(datetime.timezone.utc)
    seconds = round((now - datetime.datetime.fromisoformat(begun['started'])).total_seconds(), 3)
    quote = 'If Step 1.2 (build at t) fails twice, or Step 2 selects fewer than 3 classes, or both LLM groups end with zero methods: stop after writing the handover and `results/STATUS.md`.'
    summary = dict(status='stopped-scope-fewer-than-three-classes', started=begun['started'], finished=now.isoformat(), wall_seconds=seconds,
                   phase_seconds={'step1_setup_scope_dataset_and_verification': seconds}, selected_classes=scope['classes'], focal_methods=80,
                   developer_denominator=185, llm_denominators={'ktester': None, 'ouedraogo': None},
                   model_call_counts={'ktester': 0, 'ouedraogo': 0}, generation_rounds=[], phase_a='not run', phase_b='not run',
                   worker_counts={'phase_a': 0, 'phase_b': 0}, oom_events=[], quarantine_events=[],
                   stop_source='Run 1 docs/cc-instructions-source.md, Retries and stop conditions; inherited by run 2', stop_instruction=quote,
                   reuse_source_commit=manifest['source_commit'], copied_files_verified=len(manifest['files']), frozen_files_verified=len(frozen),
                   developer_method_kinds=dict(collections.Counter(r['kind'] for r in dev)),
                   author_code_unchanged=True, cache_trees_duplicated=False, deviations=[],
                   execution_notes=['Initial local provenance-label update used the KTester working directory and failed before writing; corrected in the fork. No build, test, dataset, or model request failed.'],
                   remaining_steps='Steps 2–7 are not run because the inherited mandatory stop is met. No survival or coverage results fabricated.', pushed=False)
    write_json(RESULTS/'final-summary.json', summary)
    status = '# Run 2 status\n\nSTOP: scope contains fewer than three classes. Step 1 populations and dataset preparation are complete; Steps 2–7 were not run.\n\n'
    status += 'The run 2 order is JsonSchema (c=26, m=23), SchemaValidatorsConfig (c=17, m=57), BaseJsonValidator (c=17, m=15), then the remaining ranked classes. The first two reach m=80, satisfying the >=50 target and exactly filling the cap. Adding the third would give m=95, exceeding the cap.\n\n'
    status += 'Run 2 explicitly inherits run 1 retries and stop conditions. Run 1 states:\n\n> '+quote+'\n\n'
    status += 'The “fewer than 3 classes” scope condition applies to run 2 Step 1 (renumbered from run 1 Step 2). The run 2 exception for cumulative m < 50 does not apply because m=80.\n\n'
    status += 'Dev: 185 stable methods. KTester population: 80 unique final IDs, one raw ID collision corrected. KTester and Ouédraogo retained denominators: unavailable (generation not run). Model calls: 0. Phase A/B: not run. No author code changed; no push.\n'
    (RESULTS/'STATUS.md').write_text(status)
    handover = '# Run 2 final handover\n\n'+status+'\n## Verified execution record\n\n```json\n'+json.dumps(summary, indent=2)+'\n```\n\n'
    handover += 'To proceed with these exact scope/cap rules, an explicit change to the inherited minimum-three-classes stop condition is needed. Do not silently add or skip classes. Copied downstream adapters retain run 1 semantics; before a future continuation, apply the run 2 one-round KTester label, project-wide whole-suite Dev compilation, compile-failure categorization, and run 2 reporting changes. No downstream tool has run.\n'
    (EXP/'docs/handover-part1.md').write_text(handover)
    (EXP/'docs/handover-final.md').write_text(handover)
    (EXP/'README.md').write_text('''# Commit-survival experiment, run 2

**Stopped after Step 1 under the inherited minimum-three-classes condition.** No model calls, survival runs, or coverage runs were performed.

The new change-ranked rule selects `JsonSchema` (23 methods) followed by `SchemaValidatorsConfig` (57), reaching 80 methods in two classes. The third ranked class would raise the total to 95. Run 1's inherited stop condition requires stopping when fewer than three classes are selected. See [status](results/STATUS.md) for the exact rule and [scope](population/scope.md) for all 40 eligible classes in rank order.

The project-wide developer denominator contains all 185 methods stable in run 1. The KTester population contains 80 methods with unique final IDs, prepared by DatasetPrepare and the original ID-renumbering procedure; raw metadata is retained. The LLM retained denominators are unavailable because generation was not run.

Anchor: `0c14a7a8664334fd10108895f84d8232f6c34019` (tag `1.1.0`). The reused range contains 229 first-parent commits ending at `1b57102e7c13acbfc9c82be20693efafb3983256`, with 181 required coverage indices. These are copied inputs, not run 2 measurements.

Files: `population/` holds the scope, methods and dataset; `dev/` holds the full frozen suite and denominator; `results/reuse.md` and `reuse-manifest.json` record copied inputs; `results/final-summary.json` records timing, validation and the stop; `tools/ORIGIN.md` records tool provenance. Local handovers are in ignored `docs/handover-*.md`. Run 1 checkout and classpath caches are reused in place without duplication.

Step 1 commands, from KTester/docker, after copying inputs as recorded in `results/reuse.md`:

```sh
run() { docker compose -f compose.yml -f compose.commit-survival.yml run --rm ktester "$@"; }
T=/repo/experiments/commit-survival-run2/tools
run python "$T/select_scope_run2.py"
run python "$T/prepare_ktester.py" dataset
run python "$T/close_scope_stop.py"
```

The copied downstream tools retain run 1 behavior except shared-cache paths. They are archival and must not be used to claim a completed run 2. Continuation requires resolving the inherited scope stop and adapting the remaining run 2 execution differences. No push was performed.
''')
    print(json.dumps({k: summary[k] for k in ['status', 'developer_denominator', 'focal_methods', 'copied_files_verified', 'frozen_files_verified', 'model_call_counts']}), flush=True)


if __name__ == '__main__':
    main()
