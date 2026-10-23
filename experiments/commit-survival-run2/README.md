# Commit-survival experiment, run 2

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
