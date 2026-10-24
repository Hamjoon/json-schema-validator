# Run 2 addendum 4: add a coverage-scoped developer column, `dev-scope`

Run 2 compares the whole developer suite (185 methods, project-wide) with LLM groups that target five scope classes. Add a developer subset that exercises the same code, measured from existing records plus one new per-method coverage pass at the anchor. No model calls. Do not modify any existing record under `generated/`, `results/raw/`, `results/coverage/`.

## Definition

`dev-scope` = developer methods in `dev/dev-denominator.csv` (185) that, when run alone at the anchor, cover at least one executable line of a scope file. Scope files are the five in `population/scope.md` (JsonSchema, BaseJsonValidator, AnyOfValidator, OneOfValidator, JsonSchemaFactory). A `@TestFactory` or `@ParameterizedTest` method is run as one unit (all of its dynamic cases together), as everywhere else in this experiment.

## Steps

1. **Per-method coverage at the anchor.** For each of the 185 methods, run it alone against the anchor build with the JaCoCo agent (same runner, same `--select-method` mechanism as Phase B, same JVM caps and timeouts). Save `results/dev-method-coverage-t/<class>#<method>.xml` and a summary `results/dev-method-coverage-t.csv` with columns: class, method, executable scope lines covered (count), scope files touched (list), total lines covered, seconds, status. Four workers. A method that fails alone at the anchor (should not happen; all 185 passed 3/3 in the suite) is recorded with status and excluded from `dev-scope`; report the count.
2. **Denominator.** `dev/dev-scope-denominator.csv` = rows with scope lines covered > 0. Report the count and the per-file distribution (how many methods touch each scope file). Record the list of the 185 − n excluded methods in `dev/dev-scope-excluded.csv`.
3. **Survival, no reruns.** Derive `dev-scope` survival by filtering the existing `results/raw/<index>-dev.json` records to the `dev-scope` methods. Write `results/raw-derived/<index>-dev-scope.json` (same schema, marked `derived_from: dev`). Cumulative rule and first-failure kinds are inherited unchanged.
4. **Changed-line coverage, reruns for this group only.** At the same 181 coverage indices, run the `dev-scope` methods alive entering each index with the JaCoCo agent, exactly as Phase B did for the other groups. Save `results/coverage/<index>-dev-scope.*`. The executable line sets must match the existing groups at every index.
5. **Tables.** Regenerate tables 1–4, `coverage-t.md`, the figure and `consistency.md` with the new column placed right after Dev: Dev, Dev (scope), KTester, Ouédraogo, Ouédraogo (adjusted), Ouédraogo (adjusted 2). Raw record check: 229 × 5 measured + 229 derived; coverage 181 × 6. In table 3 and table 4, `dev-scope` is a full group like the others.
6. `README.md`: one paragraph defining `dev-scope` and pointing to `dev-method-coverage-t.csv`. `docs/handover-part10.md`: counts (denominator, excluded, per-file distribution), timings, any method that failed alone, and the Dev vs Dev (scope) survival endpoints. Commit, no push.

## Stop conditions

None beyond run 2's. If `dev-scope` has fewer than 20 methods, still complete all steps and say so in the handover.
