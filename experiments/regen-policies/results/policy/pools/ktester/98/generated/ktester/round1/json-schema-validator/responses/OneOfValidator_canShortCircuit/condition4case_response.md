{
  "testCases": [
    {
      "group": "canShortCircuit_initialEvaluation",
      "cases": [
        {
          "input": [],
          "expected": true,
          "description": "When `canShortCircuit` is null and the parent schema's validators list does **not** contain any validator with keyword \"unevaluatedProperties\" or \"unevaluatedItems\", the method should compute and return `true`."
        },
        {
          "input": [],
          "expected": false,
          "description": "When `canShortCircuit` is null and the parent schema's validators list contains a validator whose `getKeyword()` returns \"unevaluatedProperties\", the method should compute and return `false`."
        },
        {
          "input": [],
          "expected": false,
          "description": "When `canShortCircuit` is null and the parent schema's validators list contains a validator whose `getKeyword()` returns \"unevaluatedItems\", the method should compute and return `false`."
        },
        {
          "input": [],
          "expected": false,
          "description": "When `canShortCircuit` is null and the parent schema's validators list contains both \"unevaluatedProperties\" and \"unevaluatedItems\" keywords, the method should compute and return `false` (the first match already forces the flag to false)."
        }
      ]
    },
    {
      "group": "canShortCircuit_cachedValue",
      "cases": [
        {
          "input": [],
          "expected": true,
          "description": "When `canShortCircuit` has already been cached as `true`, the method should return `true` without re‑evaluating the parent schema's validators."
        },
        {
          "input": [],
          "expected": false,
          "description": "When `canShortCircuit` has already been cached as `false`, the method should return `false` without re‑evaluating the parent schema's validators."
        }
      ]
    }
  ]
}