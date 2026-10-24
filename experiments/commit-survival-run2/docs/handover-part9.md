# Step 9 handover: corrected adjusted Ouédraogo

Completed one round with exactly 25 model calls and no retry. No push.

The dependency renderer now emits type parameters only when present, preserves declaration modifiers (including implicit public/static modifiers of interface member types), and preserves varargs as Type... instead of arrays. All five dependency blocks passed the mandatory pre-call gate; their type order, membership, omitted types, caps, API order, and prompt constraints match addendum 2. Only the requested header and varargs formatting changed.

The pre-call gate initially caught missing implicit public/static modifiers on CompositeURITranslator (an interface member type). This was fixed before any model call and covered by a regression fixture. The successful gate checks five blocks, 61 type declarations, 20 varargs parameter occurrences, unchanged dependency manifests, and unchanged prompt text outside the permitted differences. See `generated/ouedraogo-adj2/round1/prompts/block-diff.md`, `pre-call-check.json`, and `renderer-regression.json`.

Funnel: 25 extracted files, 7 compiled, 313 methods, 41 stable 3/3, 0 duplicates, **41 retained**.

| Prompt | Files | Compiled | Methods | Stable 3/3 | Duplicates | Kept |
|---|---:|---:|---:|---:|---:|---:|
| ZSL | 5 | 2 | 56 | 13 | 0 | 13 |
| FSL | 5 | 2 | 37 | 5 | 0 | 5 |
| CoT | 5 | 1 | 75 | 0 | 0 | 0 |
| ToT | 5 | 2 | 62 | 23 | 0 | 23 |
| GToT | 5 | 0 | 83 | 0 | 0 | 0 |

Import repair added 29 imports across 13 files. Initial compilation plus at most two import-only repair passes; exact edits replay successfully against the preserved `.java.pre-repair` siblings. See `generated/ouedraogo-adj2/round1/import-fixes.json`.

## Timings

Generation: 734.123 seconds. Measured wall time: 1325.450 seconds. Wall time starts at the evidence preservation snapshot; preliminary instruction review is excluded.

| Stage | Seconds |
|---|---:|
| imports | 15.938 |
| screen | 15.763 |
| diagnostics | 0.038 |
| control | 3.930 |
| phase-a | 325.109 |
| survival | 2.965 |
| phase-b | 78.798 |
| coverage | 9.358 |
| verify | 17.041 |

## Results and verification

Denominators: {'dev': 185, 'ktester': 131, 'ouedraogo': 0, 'ouedraogo-adj': 22, 'ouedraogo-adj2': 41}. Final survivors: {'dev': 4, 'ktester': 0, 'ouedraogo': 0, 'ouedraogo-adj': 0, 'ouedraogo-adj2': 0}.

Retained by class: {'com.networknt.schema.BaseJsonValidatorTest': 5, 'com.networknt.schema.JsonSchemaTest': 1, 'com.networknt.schema.JsonSchemaFactoryTest': 35}. Retained by prompt: {'FSL': 5, 'ToT': 23, 'ZSL': 13}.

First failures: `[{"index": 2, "kind": "compile-fail", "methods": 1}, {"index": 4, "kind": "compile-fail", "methods": 27}, {"index": 5, "kind": "fail", "methods": 1}, {"index": 10, "kind": "compile-fail", "methods": 12}]`.

Anchor control passed. 1145 survival records and 905 coverage records; executable line sets match across all five groups at every one of the 181 indices. All 9753 preserved evidence files and all four earlier groups' report values are unchanged. Exactly 25 request bodies match the frozen prompts, and the successful gate timestamp precedes every model call.

## Failures and limitations

Generation statuses: {'ok': 25}; finish reasons: {'stop': 25}. Compile-failure categories (overlap allowed): {'method-or-constructor-signature': 6, 'abstract-or-override-contract': 12, 'incompatible-types': 3, 'unresolved-symbol': 5, 'inaccessible-member-or-constructor': 3, 'parse-failure': 1}.

Failed measurement-stage attempts: []. The preflight failure was fixed before calls; it did not consume model calls. No retries were made for compilation failures or generation outputs.

The earlier `ouedraogo-adj` group still has its documented defective dependency headers; it is retained unchanged as requested. This new group uses the corrected blocks. All retained tests remain selected from their original generated classes; a later class-level compile failure removes all still-alive denominator methods in that class, as in the original experiment.

Detailed evidence: `results/addendum3-summary.json`, `results/addendum3-audit.json`, `results/final-verification.json`, and `results/addendum3-events.json`. Previous final summaries remain under `results/before-addendum3-*`.
