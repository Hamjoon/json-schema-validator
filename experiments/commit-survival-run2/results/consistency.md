# Consistency checks

```json
{
  "measured_raw_records": 1145,
  "derived_raw_records": 229,
  "expected_measured_raw_records": 1145,
  "expected_derived_raw_records": 229,
  "nonincreasing": true,
  "index_zero_equals_denominators": true,
  "raw_records": 1145,
  "expected_raw_records": 1145,
  "raw_count_matches": true,
  "total_survival_records": 1374,
  "expected_total_survival_records": 1374,
  "missing_records": [],
  "build_fail_indices": []
}
```

Coverage: identical executable line sets across all 6 groups at 181 indices. Required indices: 181. Missing measurements: 0 (enumerated in coverage-consistency.json).

## Six-group checks

| Check | Dev | Dev (scope) | KTester | Ouédraogo | Ouédraogo (adjusted) | Ouédraogo (adjusted 2) |
|---|---:|---:|---:|---:|---:|---:|
| Fixed denominator | 185 | 153 | 131 | 0 | 22 | 41 |
| Final survivors | 4 | 0 | 0 | 0 | 0 | 0 |
| Measured survival records | 229 | 0 | 229 | 229 | 229 | 229 |
| Derived survival records | 0 | 229 | 0 | 0 | 0 | 0 |
| Coverage records | 181 | 181 | 181 | 181 | 181 | 181 |
| Matching executable universes | 181 | 181 | 181 | 181 | 181 | 181 |

Measured raw records: 229 × 5 = 1145. Derived records: 229. Total survival records: 1374. Coverage: 181 × 6 = 1086. All 10991 original evidence files and all five earlier groups' report values are unchanged.
