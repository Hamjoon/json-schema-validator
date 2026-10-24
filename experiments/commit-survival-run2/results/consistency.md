# Consistency checks

```json
{
  "nonincreasing": true,
  "index_zero_equals_denominators": true,
  "raw_records": 1145,
  "expected_raw_records": 1145,
  "raw_count_matches": true,
  "missing_records": [],
  "build_fail_indices": []
}
```

Coverage: identical executable line sets across all 5 groups at 181 indices. Required indices: 181. Missing measurements: 0 (enumerated in coverage-consistency.json).

## Five-group checks

| Check | Dev | KTester | Ouédraogo | Ouédraogo (adjusted) | Ouédraogo (adjusted 2) |
|---|---:|---:|---:|---:|---:|
| Fixed denominator | 185 | 131 | 0 | 22 | 41 |
| Final survivors | 4 | 0 | 0 | 0 | 0 |
| Survival records | 229 | 229 | 229 | 229 | 229 |
| Coverage records | 181 | 181 | 181 | 181 | 181 |
| Matching executable universes | 181 | 181 | 181 | 181 | 181 |

All 9753 previously archived evidence files and all four previous groups' report values remain unchanged.
