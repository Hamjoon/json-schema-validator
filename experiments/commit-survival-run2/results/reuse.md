# Run 1 reuse

Copied from `experiments/commit-survival/` at commit `c4c624a7ce2675c7c91d4c03917d25394c4bdf10` without recomputing:

- `results/anchor.json`
- `results/commits.csv`
- `results/builds`
- `results/commits-check.md`
- `results/changes`
- `dev/dev-methods-all.csv`
- `dev/src-test-t`
- `dev/frozen-source-sha256.json`
- `tools`
- `population/class-survey.csv`
- `results/source-inventory.json`
- `results/coverage-commits.csv`

`reuse-manifest.json` records every copied file and its original SHA-256, before run 2 adapters. The source inventory supplies existing method eligibility; the coverage-index list retains the 181 previously derived indices.

Reuse in place: `../commit-survival/checkouts/` and `../commit-survival/work/classpaths/`, plus anchor classpath, runner jars, helper classes, Python packages and fixture runtime under run 1 work/. No checkout or classpath trees copied or symlinked. KTester snapshot and patched index stay under `/work/data/commit-survival/json-schema-validator/`. Only dataset metadata is regenerated.

The copied anchor retains its original timestamp; run 2 timing starts in `run2-start.json`. No builds, baseline tests, inventory extraction, index construction, or changed-line enumeration are repeated.
