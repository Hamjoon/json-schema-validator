{
  "testCases": [
    {
      "group": "canShortCircuit - initial evaluation, mixed validators including blocking keyword",
      "cases": [
        {
          "input": [],
          "expected": false,
          "description": "When `canShortCircuit` is null and the parent schema contains several validators, some of which have the keywords \"unevaluatedProperties\" or \"unevaluatedItems\" among other non‑blocking validators, the method must detect the blocking keyword and set `canShortCircuit` to false."
        }
      ]
    },
    {
      "group": "canShortCircuit - initial evaluation, multiple non‑blocking validators",
      "cases": [
        {
          "input": [],
          "expected": true,
          "description": "When `canShortCircuit` is null and the parent schema contains multiple validators but none of them return the keywords \"unevaluatedProperties\" or \"unevaluatedItems\", the method should compute and cache `true`."
        }
      ]
    },
    {
      "group": "canShortCircuit - initial evaluation, validator with null keyword",
      "cases": [
        {
          "input": [],
          "expected": true,
          "description": "When a validator in the parent schema returns `null` (or an empty string) for `getKeyword()`, it must be ignored as a non‑blocking keyword, resulting in `canShortCircuit` being true."
        }
      ]
    },
    {
      "group": "canShortCircuit - cached true persists after parent schema changes",
      "cases": [
        {
          "input": [],
          "expected": true,
          "description": "After the first call computes `true` and caches it, modify the parent schema to add a validator with a blocking keyword. A subsequent call must return the cached `true` without re‑evaluating the updated validators list."
        }
      ]
    },
    {
      "group": "canShortCircuit - cached false persists after parent schema changes",
      "cases": [
        {
          "input": [],
          "expected": false,
          "description": "After the first call computes `false` (because a blocking keyword was present) and caches it, change the parent schema to remove the blocking validator. A later call must still return the cached `false`."
        }
      ]
    }
  ]
}