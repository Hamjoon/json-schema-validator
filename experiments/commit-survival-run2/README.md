# Commit-survival experiment, run 2

This run compares project-wide frozen developer tests with independently generated KTester and Ouédraogo tests at tag `1.1.0`, across 229 first-parent commits. The developer denominator is all 185 methods stable in run 1. Abstract production classes remain eligible; classes dominated by trivial accessors are excluded.

Denominators: **Dev 185, KTester 131, Ouédraogo 0, Ouédraogo (adjusted) 22, Ouédraogo (adjusted 2) 41**. KTester/Dev ratio: 0.7081. Final survivors: **Dev 4, KTester 0, Ouédraogo 0, Ouédraogo (adjusted) 0, Ouédraogo (adjusted 2) 0**.

Scope: `JsonSchema`, `BaseJsonValidator`, `AnyOfValidator`, `OneOfValidator`, `JsonSchemaFactory`; 64 focal methods. [Ranked scope](population/scope.md) and [accessor audit](population/accessor-audit.csv) explain why the executable rule yields 64 methods rather than the illustrative 73-method expectation. The addendum removes the minimum class count. The superseded stopped state is retained under `results/stopped-step1/` and local `docs/handover-part1-stopped.md`.

Anchor: `0c14a7a8664334fd10108895f84d8232f6c34019`. Range endpoint: `1b57102e7c13acbfc9c82be20693efafb3983256`. Build failures are excluded and carry survival forward. [Reuse provenance](results/reuse.md) and `reuse-manifest.json` document the copied run 1 inputs. Checkouts, classpath caches and runtime libraries are reused in place.

Results: [survival events](results/table1-survival-by-event.md), [breaking commits](results/table2-breaking-commits.md), [changed-line coverage](results/table3-changed-line-coverage.md), [exposure versus break](results/table4-exposure-vs-break.md), [anchor coverage](results/coverage-t.md), [figure](results/figure1-survival-by-commit.png), [consistency](results/consistency.md), [final verification](results/final-verification.json). `results/final-summary.json` contains timings, model-call counts, failures, quarantine events and final counts.



No methods survived generation screening for ouedraogo. Their survival percentages are n/a; no survival curve can be estimated. Coverage from an empty retained suite is zero and does not establish comparative test effectiveness. See every original/retry funnel and compiler diagnostic report.

`dev/` holds the full frozen Java sources and fixtures; `population/` contains the audited scope and exact dataset; `generated/` preserves prompts, responses, generated files and independent three-run screening evidence. Per-prompt funnels are beside each round. `results/ouedraogo-round1-compile-failures.json` categorizes compiler failures without modifying prompts or generated sources. `tools/ORIGIN.md` records copied and adapted tooling. Earlier local step/final handovers remain ignored under `docs/`; the requested [part 8 handover](docs/handover-part8.md) is tracked.

Every Dev measurement attempts compilation of the whole frozen Java suite. If that fails, class-isolated compilation attributes failure only to affected classes, preserving run 1's first-failure rule. Successful whole-suite compilation is shared by separate class launches; each class retains its 120-second timeout. All 185 denominator methods are evaluated in Phase A. Phase B runs only methods alive entering each index. Fail/error outcomes receive one rerun; a passing rerun remains alive and is marked flaky. Cumulative survival uses fixed denominators. Compile failures apply to every denominator method in that class. Unavailable measurements are never converted to zero.

Changed-line coverage uses post-image executable lines, split all/scope/other files. Exposure uses pre-image lines at c−1. Scope paths stay frozen to the anchor. The consistency checks compare executable line sets across all five groups.

## Reproduction

All Java builds/tests and model calls execute in the KTester container. Copy run 1 inputs as listed in `results/reuse.md`, retaining run 1 caches. Use the existing compose configuration and credential environment file; never commit credentials. From KTester/docker:

```sh
run() { docker compose -f compose.yml -f compose.commit-survival.yml run --rm ktester "$@"; }
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

## Adjusted Ouédraogo group (addendum 2)

The single `ouedraogo-adj` round applies three adjustments together: the exact API-version / real-object constraint after each class source; anchor-source dependency constructor and public/protected method signatures, capped at 15 types and 400 lines; and deterministic repair of uniquely resolved missing-class imports, with at most two passes and no other source edits. See the [exact per-class prompt blocks and omission manifests](generated/ouedraogo-adj/round1/prompts/), [import fixes and compiler passes](generated/ouedraogo-adj/round1/import-fixes.json), [funnel](generated/ouedraogo-adj/round1/funnel.md), and [part 8 handover](docs/handover-part8.md). Pre-repair sources are stored beside repaired sources. Exactly 25 calls used `openai/gpt-oss-120b`, temperature 0.7 and max_tokens 16384; no retry calls or rounds were made. Original Dev, KTester and Ouédraogo generation and measurement records are reused unchanged, verified by SHA-256.

The archived dependency blocks have a rendering defect in synthetic type headers: non-generic types appear with empty `<>`, and type declaration modifiers are omitted. The JDK parser also normalizes varargs parameter types to arrays. Constructor/method signatures came from the anchor source, but these blocks are not compilable Java stubs. The defect was discovered after generation started; all 25 frozen prompts were preserved and no replacement calls were made. Interpret this adjusted group with that prompt limitation.

Reproduce the added stages inside the existing KTester container (generation refuses existing call directories):

```sh
run python "$T/adjusted_prompts.py"
run python "$T/generate_adjusted.py" --round 1
run python "$T/continue_adjusted.py"
run python "$T/audit_adjusted.py"
run python "$T/close_adjusted.py"
```

## Corrected adjusted Ouédraogo group (addendum 3)

The dependency renderer now emits type parameters only when present, preserves declaration modifiers (including implicit public/static modifiers of interface member types), and preserves varargs as Type... instead of arrays. All five dependency blocks passed the mandatory pre-call gate; their type order, membership, omitted types, caps, API order, and prompt constraints match addendum 2. Only the requested header and varargs formatting changed. See the [block diff](generated/ouedraogo-adj2/round1/prompts/block-diff.md), [pre-call gate](generated/ouedraogo-adj2/round1/prompts/pre-call-check.json), [import-fix ledger](generated/ouedraogo-adj2/round1/import-fixes.json), [funnel](generated/ouedraogo-adj2/round1/funnel.md), and [part 9 handover](docs/handover-part9.md).

The single `ouedraogo-adj2` round used exactly 25 calls, no retries, with the same model/settings and screening rules. 7 of 25 extracted files compiled; 41 methods were retained. Previous groups, including `ouedraogo-adj`, remain unchanged. The five-group reports contain 1145 survival records and 905 coverage records.

Reproduction inside the same KTester container (the pre-call check is mandatory and generation refuses existing call directories):

```sh
run python "$T/adjusted2_prompts.py"
run python "$T/generate_adjusted2.py" --round 1
run python "$T/continue_adjusted2.py"
run python "$T/audit_adjusted2.py"
run python "$T/close_adjusted2.py"
```
