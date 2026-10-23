# Generation funnel

```json
{
  "group": "ouedraogo",
  "round": "round1",
  "files": 25,
  "compiled": 2,
  "methods": 296,
  "stable_3_of_3": 23,
  "duplicates": 0,
  "kept_this_round": 23,
  "total_kept": 23,
  "seconds": 9.321,
  "per_prompt": {
    "ZSL": {
      "files": 5,
      "compiled": 0,
      "methods": 49,
      "stable": 0
    },
    "FSL": {
      "files": 5,
      "compiled": 1,
      "methods": 58,
      "stable": 10
    },
    "CoT": {
      "files": 5,
      "compiled": 0,
      "methods": 70,
      "stable": 0
    },
    "ToT": {
      "files": 5,
      "compiled": 1,
      "methods": 60,
      "stable": 13
    },
    "GToT": {
      "files": 5,
      "compiled": 0,
      "methods": 59,
      "stable": 0
    }
  }
}
```

Exact whitespace-stripped body hashes are deduplicated within and across rounds. Original source files remain preserved; only kept methods enter the denominator and method selections.
