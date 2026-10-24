# Generation funnel

```json
{
  "group": "ouedraogo-adj",
  "round": "round1",
  "files": 25,
  "compiled": 8,
  "methods": 320,
  "stable_3_of_3": 22,
  "duplicates": 0,
  "kept_this_round": 22,
  "total_kept": 22,
  "seconds": 14.913,
  "per_prompt": {
    "ZSL": {
      "files": 5,
      "compiled": 1,
      "methods": 57,
      "stable": 0,
      "duplicates": 0,
      "kept": 0
    },
    "FSL": {
      "files": 5,
      "compiled": 0,
      "methods": 63,
      "stable": 0,
      "duplicates": 0,
      "kept": 0
    },
    "CoT": {
      "files": 5,
      "compiled": 3,
      "methods": 66,
      "stable": 0,
      "duplicates": 0,
      "kept": 0
    },
    "ToT": {
      "files": 5,
      "compiled": 1,
      "methods": 58,
      "stable": 0,
      "duplicates": 0,
      "kept": 0
    },
    "GToT": {
      "files": 5,
      "compiled": 3,
      "methods": 76,
      "stable": 22,
      "duplicates": 0,
      "kept": 22
    }
  },
  "developer_denominator": 185,
  "ratio_to_dev": 0.11891891891891893
}
```

Exact whitespace-stripped body hashes are deduplicated within and across rounds. Original source files remain preserved; only kept methods enter the denominator and method selections.
