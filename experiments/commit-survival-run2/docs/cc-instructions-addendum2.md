# Run 2 addendum 2: adjusted Ouédraogo group, reuse everything else

Run 2 is complete (`experiments/commit-survival-run2/`, status completed). All 150 Ouédraogo files failed to compile, so that group has denominator 0. Add one more LLM group, `ouedraogo-adj`, generated with three adjustments, and measure it over the same commits. Dev and KTester results are reused as they are. Do not modify any existing `generated/`, `results/raw/`, `results/coverage/` record.

## Group definition

Name: `ouedraogo-adj`. Same five prompt types (ZSL, FSL, CoT, ToT, GToT), same five scope classes, same model (`openai/gpt-oss-120b`), same temperature 0.7 and max_tokens 16384, one call per prompt type per class. **One round only, 25 calls. No retry round, whatever the compile count.** Archive under `generated/ouedraogo-adj/round1/` with prompts, raw responses, extracted sources, funnel.

Three adjustments, all applied together:

1. **Prompt constraint (two sentences appended after the class source, identical for all five prompt types):**
   "Use only classes, constructors and methods that appear in the class source above or in the dependency signatures below. Do not use APIs from other versions of this library. Construct real objects through the constructors or factory methods shown instead of mocking; do not mock or subclass abstract classes or interfaces of this library."
2. **Dependency signatures appended to the prompt.** For the class under test, collect every project type it references: imports under `com.networknt.schema` (including nested types such as `CollectorContext.Scope`), same-package types used in field declarations, constructor parameters and method signatures, and the superclass chain (`BaseJsonValidator`, `JsonValidator`). For each, append a block `// public API of <FQN>` listing constructors and public/protected method signatures without bodies, extracted from the t sources with the JDK parser already used by `SourceInventory.java`. Order: superclass chain, constructor parameter types, field types, the rest. Cap at 15 types and 400 lines; if the cap cuts, keep the earlier ones and record the omitted types. Record the exact appended block per class in `generated/ouedraogo-adj/round1/prompts/`.
3. **Deterministic import repair after extraction (no model call).** For each extracted test file: compile once; for every `cannot find symbol ... class X` where `X` resolves to exactly one type in the t source inventory (top-level or nested) or to exactly one type in the test classpath packages `org.junit`, `org.junit.jupiter.api`, `org.mockito`, `org.mockito.stubbing`, `org.mockito.invocation`, `org.hamcrest`, add the import line and compile again. At most two passes. Change nothing but import lines. Record every added import per file in `generated/ouedraogo-adj/round1/import-fixes.json`, and keep the pre-repair source beside the repaired one.

Screening as before: compile, run 3 times at t, keep methods passing 3/3, remove exact-duplicate bodies. Write `funnel.md` with per-prompt-type counts and `results/llm-ouedraogo-adj-denominator.csv`. If the kept count is 0, write the compile-failure categories as in run 2 and skip the measurement steps below; still regenerate the tables so the group appears as 0.

## Measurement, new group only

1. Anchor control for `ouedraogo-adj` at t: every kept method must pass. `results/control-at-t-ouedraogo-adj.md`.
2. Phase A over all 229 commits for `ouedraogo-adj` only, same rules (cumulative, fixed denominator, flaky re-run once, build-fail carry-over). Raw records `results/raw/<index>-ouedraogo-adj.json`.
3. Phase B at the same 181 indices for `ouedraogo-adj` only, alive-entering methods, JaCoCo. `results/coverage/<index>-ouedraogo-adj.*`. The executable line sets must equal the existing three groups' sets at every index; report any mismatch.
4. Regenerate tables 1–4, `coverage-t.md`, figure and `consistency.md` with four columns: Dev, KTester, Ouédraogo, Ouédraogo (adjusted). Keep the original Ouédraogo column (all 0 / n/a). Raw record count check becomes 229 × 4 = 916; coverage 181 × 4.
5. `README.md`: add a paragraph naming the three adjustments and pointing to `import-fixes.json` and the prompt blocks. `docs/handover-part8.md` with funnel, timings, model calls (expected 25), import-fix counts, and anything that failed. Commit. Do not push.

## Stop conditions

None beyond run 2's. A zero-compile result is a result: record it and finish the table regeneration.
