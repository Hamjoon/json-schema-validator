{
  "tests": [
    {
      "group": "Duplicate schema instances",
      "cases": [
        {
          "input": [],
          "expected": "initializeValidators called once for each list entry (including duplicates); method completes without exception.",
          "description": "Add the same mock JsonSchema instance twice to the validator's `schemas` list. After invoking `preloadJsonSchema()`, verify that `initializeValidators()` is invoked twice (once per list entry), confirming that the method does not deduplicate entries."
        }
      ]
    },
    {
      "group": "Null schema element",
      "cases": [
        {
          "input": [],
          "expected": "NullPointerException thrown when iterating over a null element in `schemas`.",
          "description": "Insert a `null` reference into the validator's `schemas` list (e.g., `[mockSchema, null, mockSchema]`). Calling `preloadJsonSchema()` should result in a NullPointerException when the for‑each loop attempts to call `initializeValidators()` on the null element, demonstrating that the method does not guard against null entries."
        }
      ]
    },
    {
      "group": "Concurrent modification during iteration",
      "cases": [
        {
          "input": [],
          "expected": "ConcurrentModificationException propagates out of preloadJsonSchema.",
          "description": "Configure a mock JsonSchema whose `initializeValidators()` method adds another JsonSchema to the validator's `schemas` list. When `preloadJsonSchema()` runs, the underlying `ArrayList` will detect the structural modification during iteration and throw a ConcurrentModificationException, confirming that the method does not protect against concurrent modifications."
        }
      ]
    },
    {
      "group": "Idempotent call",
      "cases": [
        {
          "input": [],
          "expected": "initializeValidators called exactly once per schema on each invocation; no side‑effects beyond the calls.",
          "description": "Add three distinct mock JsonSchema objects to the validator's `schemas` list. Invoke `preloadJsonSchema()` twice in succession. Verify that `initializeValidators()` is called three times after the first call and three additional times after the second call (total six), demonstrating that the method can be safely called multiple times."
        }
      ]
    }
  ]
}