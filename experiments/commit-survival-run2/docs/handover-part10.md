# Step 10 handover: coverage-scoped developer group

Dev (scope) is the subset of the 185 frozen developer methods that pass when run alone at the anchor and cover at least one executable line in the five scope files. A test-factory or parameterized method is selected as one unit with all its cases. Survival is derived verbatim from the existing Dev method outcomes; coverage runs only the subgroup methods alive entering each index.

Denominator: **153**. Excluded: **32**. Exclusion reasons: {'failed-alone': 1, 'no-scope-line-covered': 31}. No model calls. No Phase A test reruns. No push.

## Scope-file distribution

A method may touch multiple scope files. Counts include only retained, passing methods.

| Scope file | Methods touching it |
|---|---:|
| src/main/java/com/networknt/schema/AnyOfValidator.java | 17 |
| src/main/java/com/networknt/schema/BaseJsonValidator.java | 147 |
| src/main/java/com/networknt/schema/JsonSchema.java | 147 |
| src/main/java/com/networknt/schema/JsonSchemaFactory.java | 153 |
| src/main/java/com/networknt/schema/OneOfValidator.java | 27 |

## Methods failing alone

- `com.networknt.schema.Issue662Test#testCorrectErrorForInvalidValue`: fail. `No validation error that the object is not expected ==> expected: <false> but was: <true>`

The other exclusions had no covered executable scope line. All 185 per-method XML reports and method-selection evidence are in `results/dev-method-coverage-t/`. Canonical summary: `results/dev-method-coverage-t.csv`; selected and excluded rows: `dev/dev-scope-denominator.csv` and `dev/dev-scope-excluded.csv`.

## Survival and coverage

Dev: **185 → 4** (2.162% at the end). Dev (scope): **153 → 0**.

The 229 derived records in `results/raw-derived/` retain each selected Dev outcome, message, retry state and flaky flag unchanged; `derived_from: dev`, source record paths and SHA-256 hashes establish provenance. No new survival tests ran. Phase B measured 181 indices with the alive-entering subgroup and the original whole-frozen-Dev compilation/fallback behavior. Its four workers ran in separate container network namespaces.

Audit passed: 10991 prior evidence files unchanged; previous group report values unchanged; 1145 measured + 229 derived survival records; 1086 coverage records; executable line sets match at all 181 indices across all six groups. See `results/addendum4-audit.json` and `results/final-verification.json`.

## Timings

Measured wall time: 7489.450 seconds. Wall time starts at the evidence snapshot and includes the invalid shared-network attempt and a brief orchestration hold to clarify a misdirected message. Worker execution durations are recorded separately.

| Stage | Seconds |
|---|---:|
| anchor-prepare | 3.086 |
| anchor-methods | 324.062 |
| anchor-finalize | 0.860 |
| derive-survival | 7.436 |
| survival-report | 7.720 |
| phase-b | 6633.844 |
| coverage-report | 44.880 |
| verify | 123.410 |

Anchor worker execution seconds: [227.582, 224.453, 228.353, 227.469]. Coverage worker execution seconds: [6632.838, 6489.517, 6571.285, 6437.012]. Shared anchor compilation: 2.243 seconds.

## Execution correction

The first attempt shared one network namespace across four concurrent JVMs. HTTPServiceSupport binds localhost:1234; 6 completed records reported BindException. The attempt was deliberately stopped after 148 records and preserved under `results/addendum4-quarantine/shared-network-anchor-attempt/`. Exit 137 was an explicit container stop, not an OOM. Those records do not enter the selection. The complete valid pass ran all 185 methods in four independent containers, with no per-method retries or source changes. See `results/addendum4-isolation-correction.json`.

A brief orchestrator hold clarified a message intended for a sibling repository. Existing isolated workers completed uninterrupted; the user said to ignore that message, and the reference addendum resumed. No sibling repository files were edited. See `results/addendum4-orchestration-note.json`.

## Instrumented outcome differences

The subgroup coverage pass has 2 method outcomes different from the inherited Phase A outcomes. They are recorded in `results/addendum4-summary.json` and `results/final-verification.json`. Survival continues to use the original Dev outcomes unchanged; coverage outcomes do not redefine it.

## Anchor scope coverage

Dev covers 620/689 executable scope lines; Dev (scope) covers 616/689. The subgroup coverage matches the union of passing isolated method coverage. The 4 lines covered only by the full Dev suite are listed in `results/addendum4-audit.json`.
