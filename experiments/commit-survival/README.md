# Commit-survival experiment

This experiment compares frozen developer tests with independently generated method-focused and five-prompt test groups. Tests are screened at tag `1.1.0` and evaluated cumulatively against the complete first-parent range to the pinned fork master.

Results: [survival events](results/table1-survival-by-event.md), [breaking commits](results/table2-breaking-commits.md), [changed-line coverage](results/table3-changed-line-coverage.md), [exposure versus break](results/table4-exposure-vs-break.md), [survival figure](results/figure1-survival-by-commit.png), and [consistency checks](results/consistency.md).

Anchor: `0c14a7a8664334fd10108895f84d8232f6c34019`. Range endpoint: `1b57102e7c13acbfc9c82be20693efafb3983256`. Range length: 229 commits. Build-fail indices are excluded from evaluation and carry survival forward.

Scope: `com.networknt.schema.PrefixItemsValidator`, `com.networknt.schema.PropertiesValidator`, `com.networknt.schema.TypeValidator`, `com.networknt.schema.CollectorContext`, `com.networknt.schema.SpecVersionDetector`. These are all eligible classes with a dedicated test file and at least three eligible declared methods. Cumulative focal methods: 29. Developer denominator: 10; method-focused denominator: 32; five-prompt denominator: 23.

Generation caveat: ktester-round1-retry: java.lang.NullPointerException: sourceMethod is null in Spoon CtMethod.prettyprint; caught and logged by author post_process.py (repair stopped at target 8 of 29; available generated files independently screened). Counts and compiler/run evidence are retained in each round's funnel and validation records. The retained denominators passed the final anchor control.

`results/` contains the anchor, full commit/build ledger, baseline reports, raw survival records, JaCoCo XML and line CSVs, four tables, figure, consistency checks, and final status. `dev/` contains frozen Java/resources/suite inputs and the developer method denominator. `population/` contains the source inventory selection and exact KTester dataset. `generated/` retains prompts, responses, original test sources, and validation funnels. `tools/ORIGIN.md` and `tools/origin.json` record copied-tool provenance and local adaptations. Ignored `work/` and `checkouts/` contain reproducible build/runtime intermediates; ignored `docs/handover-*.md` contain local handovers.

## Reproduction

All builds, tests, and model calls run in the KTester JDK 17 container. Start from the required branch bases and configure `docker/compose.commit-survival.yml` with the JSV mount at `/repo`, read-only source mounts at `/jsoup` and `/codec`, and `env_file: [.env]`. Keep the environment file untracked. Commands below run from KTester/docker; the experiment must start without prior model-round directories. Resume build/phase commands from their complete JSON records. Generation scripts refuse to overwrite completed rounds.

```sh
run() { docker compose -f compose.yml -f compose.commit-survival.yml run --rm ktester "$@"; }
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
run python "$T/audit_artifacts.py"
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
run python "$T/final_check.py"
run python "$T/close_experiment.py"
```

After each group round, compare its kept denominator to the developer denominator. Run another identical round only if smaller, up to three rounds. Retry a zero-compiled round once after 60 seconds. This run's initial KTester metadata preflight made no calls and its permitted retry is archived as `round1-retry`; validate that label when resuming this run. The final metadata adapter preserves exact index signatures. All setup failures and corrections are retained in `results/execution-notes.json`.

Each generated file is compiled independently and a method must pass three runs. Exact whitespace-stripped body duplicates are excluded from denominators without rewriting archived source. The final anchor control selects every retained method once more. Phase A uses a fixed denominator; a fail/error gets one rerun, while compilation failure affects every denominator method in that file. Phase B selects only methods alive entering an index. Coverage compares post-image added/modified executable lines, split by scope; exposure uses pre-image changed lines at the immediate predecessor. Scope membership uses fixed anchor source paths, following the copied design: code moved to another path subsequently appears in the other-files category. Missing measurements are never converted to zero. The final consistency files list all build failures and missing records. `results/artifact-audit.json` verifies frozen inputs, unchanged author code, and copied-tool hashes.

Commit the completed step artifacts locally with the required attribution trailer. After the final fork commit, run `run python "$T/record_ktester_completion.py"` to record that commit and the outcome in the isolated KTester dataset, then commit that new result file there. Do not push. Existing unrelated KTester changes must remain outside the experiment commits.
