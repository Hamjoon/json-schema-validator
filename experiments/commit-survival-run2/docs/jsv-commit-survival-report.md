# What survives when the code keeps moving? Developer, KTester and Ouédraogo tests on json-schema-validator at commit granularity, 2023 to 2026

## Question

Last week's commit-level measurement on Commons Codec saw almost no change in the code under test and almost no test loss. This week we repeat it on a project chosen for the opposite reason, networknt/json-schema-validator, which has been refactored continuously since late 2023, and run two generators side by side: KTester (Li et al., ICSE 2026) and the five prompt-only templates of Ouédraogo et al. (EMSE 2026). The questions are the same: how long do tests frozen at a starting commit *t* keep passing, and at each production change, how many of the changed lines do the still-passing tests execute.

## Setup

- **Project and range.** json-schema-validator, *t* = release 1.1.0 (`0c14a7a`, 2023-12-15). Points are the 229 consecutive first-parent commits on `master` after *t*, up to 2026-10-23. All 229 build. Release 1.2.0 is commit 6; the 2.x rewrite is commit 151. 128 commits change production code.
- **Classes under test.** Five classes chosen by how often the range changes them, excluding classes that are mostly getters and setters: `JsonSchema`, `BaseJsonValidator`, `AnyOfValidator`, `OneOfValidator`, `JsonSchemaFactory`; 64 public, non-private methods.
- **Tests, frozen at *t*.** This project's developer tests are organised by issue and by the JSON Schema test suite rather than by class, so the developer set is defined by coverage: of the 185 developer test methods that pass three runs at *t*, the 153 that, run alone, execute at least one line of the five classes (**Dev (scope)**). A parameterized or test-factory method counts as one method. **KTester**: tool as released with `gpt-oss-120b`, run once over the 64 methods; 64 files produced, 39 compiled, 523 methods listed, 131 passing three runs, no duplicates. **Ouédraogo**: the five templates as released, one call per template per class (25 files), produced no file that compiled against the five classes, in six attempts (150 files). The set reported here, **Ouédraogo (adjusted)**, adds three things to the same templates: two sentences restricting the model to the API visible in the prompt and to real objects instead of mocks, the constructor and public method signatures of the project types the class refers to, and automatic insertion of missing imports after generation; 25 files, 7 compiled, 313 methods, 41 passing three runs. In the tables the two sets are labelled simply Dev and Ouédraogo. No test was edited by hand.
- **Survival and changed-line coverage** as in the previous weeks: a method survives at commit *c* if it passed at every evaluated commit up to *c*; denominators fixed at 153, 131 and 41; a lost method is classified by its state at the first failing commit. For each of the 128 production-changing commits, the lines added or modified that JaCoCo reports as executable at *c*, and the part of them executed by each group's surviving methods. Over the whole range 11,800 executable lines changed; 1,399 of them (12%) are in the five classes, and the comparison is made on those, since the developer set reaches the other files directly while the generated sets reach them only through the five classes.

## Results

**Table 1. Surviving methods at the commits where a group lost methods** (count and percent of the fixed denominator; commits that lost one or two developer methods only are omitted; full series in the repository).

| # | Commit | Date | Change | Dev (153) | KTester (131) | Ouédraogo (41) |
|---:|---|---|---|---:|---:|---:|
| 0 | 0c14a7a (*t*) | 2023-12-15 | release 1.1.0 | 153 (100%) | 131 (100%) | 41 (100%) |
| 2 | 48ca8c3 | 2024-01-18 | Refactor validation message generation | 146 (95.4%) | 131 (100%) | 40 (97.6%) |
| 4 | 5a94df7 | 2024-01-19 | Refactor of paths | 125 (81.7%) | 82 (62.6%) | 13 (31.7%) |
| 5 | 9b73d10 | 2024-01-19 | Support schema resource | 123 (80.4%) | 81 (61.8%) | 12 (29.3%) |
| 6 | 85f112a | 2024-01-19 | **release 1.2.0** | 121 (79.1%) | 81 (61.8%) | 12 (29.3%) |
| 10 | 7f1ec11 | 2024-01-26 | Support Draft 2020-12, refactor schema retrieval | 72 (47.1%) | 51 (38.9%) | 0 (0%) |
| 18 | 85d642b | 2024-02-04 | Fix hierarchy output report | 68 (44.4%) | 49 (37.4%) | 0 |
| 38 | 95911ba | 2024-03-01 | Custom vocabularies, meta-schema handling | 64 (41.8%) | 48 (36.6%) | 0 |
| 55 | ea64777 | 2024-04-22 | Upgrade Jackson to 2.17.0 | 59 (38.6%) | 48 (36.6%) | 0 |
| 148 | db71780 | 2025-09-20 | Add benchmark | 46 (30.1%) | 48 (36.6%) | 0 |
| 150 | 02a6e06 | 2025-09-27 | Refactor set to list in execution context | 7 (4.6%) | 30 (22.9%) | 0 |
| 151 | e880dd0 | 2025-10-08 | **Main refactor for 2.x** | 0 (0%) | 0 (0%) | 0 |
| 229 | 1b57102 | 2026-10-23 | (end of range) | **0 (0%)** | **0 (0%)** | **0 (0%)** |

**Table 2. Changed-line coverage at the commits that change the five classes** (lines: executable changed lines in the five classes; covered: lines executed by the group's surviving methods at that commit; lost: methods of the group lost at that commit. The 14 commits with ten or more changed lines or with a loss are listed; the 24 others and the totals follow. The 90 production-changing commits that change only other files are not rows.)

| # | Commit | Date | Change | Lines | Dev covered | KTester covered | Ouédraogo covered | Dev lost | KTester lost | Ouédraogo lost |
|---:|---|---|---|---:|---:|---:|---:|---:|---:|---:|
| 2 | 48ca8c3 | 2024-01-18 | Refactor validation message generation | 43 | 26 (60.5%) | 7 (16.3%) | 1 (2.3%) | 7 | 0 | 1 |
| 4 | 5a94df7 | 2024-01-19 | Refactor of paths | 178 | 161 (90.4%) | 98 (55.1%) | 30 (16.9%) | 21 | 49 | 27 |
| 5 | 9b73d10 | 2024-01-19 | Support schema resource | 139 | 125 (89.9%) | 63 (45.3%) | 65 (46.8%) | 2 | 1 | 1 |
| 10 | 7f1ec11 | 2024-01-26 | Support Draft 2020-12, refactor schema retrieval | 279 | 182 (65.2%) | 110 (39.4%) | 0 (0%) | 48 | 30 | 12 |
| 13 | 9c95c06 | 2024-01-31 | Annotation support, output formats | 180 | 130 (72.2%) | 64 (35.6%) | 0 | 0 | 0 | 0 |
| 18 | 85d642b | 2024-02-04 | Fix hierarchy output report | 22 | 20 (90.9%) | 11 (50.0%) | 0 | 4 | 2 | 0 |
| 30 | 1f60740 | 2024-02-13 | Reduce memory usage | 33 | 22 (66.7%) | 12 (36.4%) | 0 | 0 | 0 | 0 |
| 33 | bd085ed | 2024-02-16 | Fixes for discriminator | 34 | 8 (23.5%) | 1 (2.9%) | 0 | 0 | 0 | 0 |
| 38 | 95911ba | 2024-03-01 | Custom vocabularies, meta-schema handling | 45 | 33 (73.3%) | 27 (60.0%) | 0 | 2 | 1 | 0 |
| 40 | 0f983b0 | 2024-03-12 | Refactor walk | 13 | 10 (76.9%) | 3 (23.1%) | 0 | 1 | 0 | 0 |
| 76 | 4e5a808 | 2024-06-12 | Make more fields final | 29 | 20 (69.0%) | 20 (69.0%) | 0 | 0 | 0 | 0 |
| 82 | 4f3345b | 2024-06-20 | Builder for `SchemaValidatorsConfig` | 37 | 18 (48.6%) | 13 (35.1%) | 0 | 0 | 0 | 0 |
| 149 | 19b2207 | 2025-09-23 | Remove validation message handler | 76 | 43 (56.6%) | 43 (56.6%) | 0 | 0 | 0 | 0 |
| 150 | 02a6e06 | 2025-09-27 | Refactor set to list in execution context | 70 | 11 (15.7%) | 5 (7.1%) | 0 | 39 | 18 | 0 |
| | 24 other commits | | 1 to 27 changed lines each | 221 | 106 (48.0%) | 76 (34.4%) | 0 | 1 | 0 | 0 |
| | **Total, five classes** | | | **1,399** | **915 (65.4%)** | **553 (39.5%)** | **96 (6.9%)** | 125 | 101 | 41 |

**Table 3. Line coverage of the five classes at *t*** (full denominators executed).

| Class | Executable lines | Dev (153) | KTester (131) | Ouédraogo (41) |
|---|---:|---:|---:|---:|
| `AnyOfValidator` | 71 | 66 (93.0%) | 48 (67.6%) | 0 (0%) |
| `BaseJsonValidator` | 95 | 87 (91.6%) | 35 (36.8%) | 17 (17.9%) |
| `JsonSchema` | 250 | 232 (92.8%) | 168 (67.2%) | 25 (10.0%) |
| `JsonSchemaFactory` | 210 | 181 (86.2%) | 151 (71.9%) | 172 (81.9%) |
| `OneOfValidator` | 63 | 50 (79.4%) | 0 (0%) | 0 (0%) |

## Observations

1. **Nothing frozen at *t* survives the range, and most losses are compile failures at three refactorings.** The developer set loses 20% at the 1.2.0 release (commits 2 to 6), half by the Draft 2020-12 refactoring (commit 10), and everything at the 2.x rewrite (commits 150 and 151); KTester follows the same three steps, Ouédraogo (adjusted) is gone at commit 10. Of the 153 developer losses 128 are compile failures, 17 assertion failures and 8 runtime exceptions; of the 131 KTester losses 119, 10 and 2; of the 41 adjusted losses 40 and 1. On Commons Codec last week there was no compile failure in 539 commits.
2. **The developer set executes more of each change and loses less at the API refactorings; between them, it is the only set that keeps losing.** Over the five classes the developer methods executed 65.4% of the 1,399 changed lines and KTester 39.5% (Table 2), and at *t* the sets cover 79 to 93% against 0 to 72% of the same files (Table 3). At the two early refactorings KTester lost 49 and 30 methods against the developer set's 21 and 48. From commit 38 to commit 148 KTester stays at 48 methods while the developer set goes from 64 to 46, through assertion failures and runtime exceptions at small behaviour changes (commits 40, 55 and others), not compile failures.
3. **Every loss happened at a commit whose changed lines the losing group was executing, and the prompt-only generator produced no usable test without help.** Of the production-changing commits whose changed lines a group executed at the previous commit, the developer set lost methods at 11 of 56, KTester at 7 of 46 and Ouédraogo (adjusted) at 4 of 4; no group lost a method at a commit whose changed lines it did not execute, as on the three previous projects. The released Ouédraogo templates gave 150 files for these five classes and none compiled: the generated code calls constructors and methods with wrong signatures, uses API that appears only in later versions of the library, and omits imports. With the three adjustments 7 of 25 files compiled and 41 methods passed, 35 of them for `JsonSchemaFactory`.

## Limitations

- The developer set is defined by coverage because this project has few per-class test classes; 153 of the 185 developer methods qualify, so it is close to the whole suite, and one of its methods (the JSON Schema test suite runner) accounts for about 6,900 of the 7,466 test cases the suite executes. Method-level survival does not weight tests by size.
- KTester compiled 39 of 64 files, and only 131 of the 523 listed methods passed three runs (25%; 73% on Commons Codec); the classes here construct and depend on many other project types.
- Ouédraogo (adjusted) departs from the published templates and rests on four generated files, two of which hold 35 of the 41 methods; the released templates are reported as producing nothing. The developer set, KTester and the adjusted set were each generated or selected once.
- The five classes are a rule-based selection, and the comparison holds inside them; the developer tests also cover the other 110 classes, where 10,401 of the 11,800 changed lines are.

## Repository

`Hamjoon/json-schema-validator`, branch `experiment/2026-10-week4-jsv-commit-survival`, directory `experiments/commit-survival-run2/`.

- Commits: `results/commits.csv`. Selection: `population/scope.md`, `dev/dev-scope-denominator.csv`, `results/dev-method-coverage-t.csv`.
- Generated tests and screening: `generated/<group>/`, `results/llm-<group>-denominator.csv`.
- Survival: `results/raw/`, `table1-survival-by-event.md`, `table2-breaking-commits.md`, `figure1-survival-by-commit.png`.
- Changed-line coverage: `results/changed-lines.csv`, `table3-changed-line-coverage.md` (all files, five classes, other files; all groups including the whole developer suite and the unadjusted Ouédraogo set), `table4-exposure-vs-break.md`, `coverage-t.md`, `consistency.md`.
- `experiments/commit-survival/` holds a first pass with a per-class developer set of 10 methods, kept as a pipeline check and not reported.
