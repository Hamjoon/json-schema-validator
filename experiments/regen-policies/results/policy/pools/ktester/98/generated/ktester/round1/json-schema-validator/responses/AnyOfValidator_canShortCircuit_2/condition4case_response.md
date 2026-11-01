{
  "testCases": [
    {
      "group": "canShortCircuit - initial evaluation, no blocking keywords",
      "cases": [
        {
          "input": [],
          "expected": true,
          "description": "When `canShortCircuit` field is null and the parent schema's validators list does not contain any validator whose keyword is \"unevaluatedProperties\" or \"unevaluatedItems\", the method should compute `true` and store it."
        }
      ]
    },
    {
      "group": "canShortCircuit - initial evaluation, contains \"unevaluatedProperties\" keyword",
      "cases": [
        {
          "input": [],
          "expected": false,
          "description": "When `canShortCircuit` field is null and at least one validator in the parent schema returns the keyword \"unevaluatedProperties\", the method must set `canShortCircuit` to false and return false."
        }
      ]
    },
    {
      "group": "canShortCircuit - initial evaluation, contains \"unevaluatedItems\" keyword",
      "cases": [
        {
          "input": [],
          "expected": false,
          "description": "When `canShortCircuit` field is null and at least one validator in the parent schema returns the keyword \"unevaluatedItems\", the method must set `canShortCircuit` to false and return false."
        }
      ]
    },
    {
      "group": "canShortCircuit - cached true value",
      "cases": [
        {
          "input": [],
          "expected": true,
          "description": "If `canShortCircuit` field has already been set to `true` (by a previous call), the method should return the cached value without iterating over validators."
        }
      ]
    },
    {
      "group": "canShortCircuit - cached false value",
      "cases": [
        {
          "input": [],
          "expected": false,
          "description": "If `canShortCircuit` field has already been set to `false` (by a previous call), the method should return the cached value without iterating over validators."
        }
      ]
    }
  ]
}