# Run 2 addendum 3: rerun the adjusted Ouédraogo group with corrected dependency blocks

Addendum 2 produced group `ouedraogo-adj` (22 methods). Its dependency-signature blocks had a rendering defect: non-generic types printed with empty `<>`, type declaration modifiers omitted, varargs shown as arrays. Fix the renderer and generate one more group, `ouedraogo-adj2`, otherwise identical to addendum 2. Keep `ouedraogo-adj` and all its records unchanged.

## Changes from addendum 2

1. **Renderer fix.** In the dependency-block generator: print type parameters only when the declaration has them; print declaration modifiers (`public`, `abstract`, `final`, `interface`/`class`/`enum`) on the type header; print varargs as `Type...`. Everything else in the block (type order, cap 15 types / 400 lines, constructor and public/protected method signatures without bodies) stays as in addendum 2. Before any model call, render the five blocks, diff them against the addendum 2 blocks, and record the diff in `generated/ouedraogo-adj2/round1/prompts/block-diff.md`. If a block still contains `<>` with nothing inside, or a header without a modifier, stop and fix before calling.
2. **Group name** `ouedraogo-adj2`. Same five prompt types, same five scope classes, same model and settings, one round, exactly 25 calls, no retry. Same two constraint sentences. Same deterministic import repair with `import-fixes.json` and `.pre-repair` siblings.

## Steps

Same as addendum 2, for `ouedraogo-adj2` only: screening (3/3, dedup) → `results/llm-ouedraogo-adj2-denominator.csv` → anchor control → Phase A over 229 commits → Phase B at the 181 indices → regenerate tables 1–4, `coverage-t.md`, figure, `consistency.md` with five columns: Dev, KTester, Ouédraogo, Ouédraogo (adjusted), Ouédraogo (adjusted 2). Raw record check 229 × 5 = 1145; coverage 181 × 5. README paragraph naming the renderer fix. `docs/handover-part9.md` with funnel per prompt type, import-fix counts, timings, model calls (expected 25). Commit, no push.

## Stop conditions

Only the pre-call block check above. A zero-compile result is recorded and the tables are still regenerated.
