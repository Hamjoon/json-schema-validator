# Step 8 handover: adjusted Ouédraogo

Completed one round, exactly 25 model calls, zero retry calls. No push.

Funnel: 25 files, 8 compiled, 320 extracted methods, 22 stable 3/3, 0 exact-body duplicates, **22 kept**.

Import repair: 20 added import lines across 11 files. See `generated/ouedraogo-adj/round1/import-fixes.json`; each source has a `.java.pre-repair` sibling.

| Prompt | Files | Compiled | Methods | Stable 3/3 | Duplicates | Kept |
|---|---:|---:|---:|---:|---:|---:|
| ZSL | 5 | 1 | 57 | 0 | 0 | 0 |
| FSL | 5 | 0 | 63 | 0 | 0 | 0 |
| CoT | 5 | 3 | 66 | 0 | 0 | 0 |
| ToT | 5 | 1 | 58 | 0 | 0 | 0 |
| GToT | 5 | 3 | 76 | 22 | 0 | 22 |

## Timings

Generation: 550.888 seconds. Measured addendum wall time: 932.171 seconds. Wall clock starts at the preservation manifest; preliminary instruction review/tool preparation is excluded.

| Stage | Seconds |
|---|---:|
| imports | 14.665 |
| screen | 14.953 |
| diagnostics | 0.049 |
| control | 1.458 |
| phase-a | 97.045 |
| survival | 2.642 |
| phase-b | 70.393 |
| coverage | 6.833 |
| verify | 17.016 |

## Results and verification

Denominators: {'dev': 185, 'ktester': 131, 'ouedraogo': 0, 'ouedraogo-adj': 22}. Final survivors: {'dev': 4, 'ktester': 0, 'ouedraogo': 0, 'ouedraogo-adj': 0}.

Adjusted losses: one cache-identity assertion first failed at index 5; the remaining 21 methods were lost to class compilation failure at index 10 after URI APIs changed. The fixed denominator remains 22 throughout.

Anchor control passed. 916 survival records and 724 coverage records; four-group executable universes agree at all 181 indices. 8494 original files passed the preservation hash check. Import changes replay exactly; all 25 request bodies match their frozen prompts and settings.

## Failures and limitations

The archived dependency blocks have a rendering defect in synthetic type headers: non-generic types appear with empty `<>`, and type declaration modifiers are omitted. The JDK parser also normalizes varargs parameter types to arrays. Constructor/method signatures came from the anchor source, but these blocks are not compilable Java stubs. The defect was discovered after generation started; all 25 frozen prompts were preserved and no replacement calls were made. Interpret this adjusted group with that prompt limitation.

Compile-failure categories (overlap allowed): `{"abstract-or-override-contract": 6, "method-or-constructor-signature": 5, "inaccessible-member-or-constructor": 7, "unresolved-symbol": 7, "incompatible-types": 2, "missing-package-or-import": 1, "unknown-or-external-type-unresolved": 2, "other-compile": 1}`.

Generation statuses: {'ok': 25}; finish reasons: {'stop': 25}. Model calls are not repeated for compile failures or truncated responses. All 22 retained methods came from the JsonSchemaFactory GToT file. Other compiled files failed screening primarily because generated fixtures supplied null URIFactory or ValidationContext objects.

The first Phase A launch failed before any measurement because argparse interpreted the hyphen-prefixed result suffix as an option. It was corrected to `--result-suffix=-ouedraogo-adj`, then Phase A resumed without model calls or test reruns. The failed launch remains in addendum2-events.json.

Early preparation failures were corrected before model calls: the inventory reader initially encountered the JVM environment banner, and the prompt insertion check initially assumed every template ended at the source. FSL/ToT/GToT contain trailing template text; the final insertion preserves it exactly. A host setup command initially used the KTester working directory for repository-relative writes and failed without creating files.

All detailed timings, audit evidence, and record counts are in `results/addendum2-summary.json`, `results/addendum2-audit.json`, and `results/final-verification.json`. The previous final summaries are retained as `results/before-addendum2-*`.
