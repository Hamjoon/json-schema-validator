{
  "testCases": [
    {
      "group": "Multiple consecutive preload calls",
      "cases": [
        {
          "input": [],
          "expected": "After first call, schemas list size is 2; after second call, size is 4 (each call adds two schemas).",
          "description": "A test subclass of AnyOfValidator overrides preloadJsonSchemas to add exactly two mock JsonSchema instances to the supplied list. The test invokes preloadJsonSchema() twice and asserts that the internal list grows cumulatively, confirming that the method forwards the same mutable list on each call and does not reset it."
        }
      ]
    },
    {
      "group": "Preload clears existing schemas",
      "cases": [
        {
          "input": [],
          "expected": "schemas list becomes empty after preloadJsonSchema() is called.",
          "description": "A test double pre‑populates the internal schemas list with three mock schemas and overrides preloadJsonSchemas to invoke schemas.clear(). After calling preloadJsonSchema(), the list should be empty, verifying that the method correctly forwards the list even when the delegate clears it."
        }
      ]
    },
    {
      "group": "Preload adds null schema",
      "cases": [
        {
          "input": [],
          "expected": "schemas list size increases by one and the newly added element is null.",
          "description": "A subclass overrides preloadJsonSchemas to add a null entry to the supplied list. The test checks that the list contains a null element after preloadJsonSchema() execution, ensuring that the method does not guard against nulls added by the delegate."
        }
      ]
    },
    {
      "group": "Preload with large number of schemas",
      "cases": [
        {
          "input": [],
          "expected": "schemas list size becomes 100 after preloadJsonSchema() is called.",
          "description": "The test subclass overrides preloadJsonSchemas to add 100 distinct mock JsonSchema objects to the list. This validates that preloadJsonSchema() can handle a large number of additions without side‑effects such as truncation or overflow."
        }
      ]
    },
    {
      "group": "Preload with no modifications",
      "cases": [
        {
          "input": [],
          "expected": "schemas list size remains unchanged after preloadJsonSchema() is called.",
          "description": "A subclass overrides preloadJsonSchemas with an empty implementation (does nothing). The internal list is pre‑filled with five mock schemas. After invoking preloadJsonSchema(), the list size should still be five, confirming that the method does not alter the list when the delegate makes no changes."
        }
      ]
    }
  ]
}