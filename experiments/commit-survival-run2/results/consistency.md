# Consistency checks

```json
{
  "nonincreasing": true,
  "index_zero_equals_denominators": true,
  "raw_records": 916,
  "expected_raw_records": 916,
  "raw_count_matches": true,
  "missing_records": [],
  "build_fail_indices": []
}
```

Coverage: identical executable line sets across all 4 groups at 181 indices. Required indices: 181. Missing measurements: 0 (enumerated in coverage-consistency.json).

## Four-group checks

| Check | Dev | KTester | Ouédraogo | Ouédraogo (adjusted) |
|---|---:|---:|---:|---:|
| Fixed denominator | 185 | 131 | 0 | 22 |
| Final survivors | 4 | 0 | 0 | 0 |
| Survival records | 229 | 229 | 229 | 229 |
| Coverage records | 181 | 181 | 181 | 181 |
| Matching executable universes | 181 | 181 | 181 | 181 |

Original group report values and all original generated/raw/coverage files are unchanged. The preservation audit passed.
