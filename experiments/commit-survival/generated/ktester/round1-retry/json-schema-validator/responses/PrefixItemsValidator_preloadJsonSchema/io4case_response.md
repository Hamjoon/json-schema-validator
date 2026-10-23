{
  "testCases": [
    {
      "group": "Preload with single schema",
      "cases": [
        {
          "input": [],
          "expected": "No exception thrown; preloadJsonSchemas is called exactly once with a list containing the single JsonSchema instance, preserving its reference",
          "description": "When PrefixItemsValidator is constructed with a tupleSchema list that holds one JsonSchema, calling preloadJsonSchema() must forward that list to preloadJsonSchemas without alteration."
        }
      ]
    },
    {
      "group": "Preload with duplicate schemas",
      "cases": [
        {
          "input": [],
          "expected": "No exception thrown; preloadJsonSchemas is called once with a list containing the duplicate JsonSchema objects in the original order",
          "description": "Validate that duplicate entries in tupleSchema are not deduplicated by preloadJsonSchema() and the exact list (including duplicates) is passed to preloadJsonSchemas."
        }
      ]
    },
    {
      "group": "Preload propagates exception from schema preload",
      "cases": [
        {
          "input": [],
          "expected": "RuntimeException thrown from preloadJsonSchemas is propagated to the caller of preloadJsonSchema()",
          "description": "If any JsonSchema in tupleSchema causes preloadJsonSchemas to throw a RuntimeException, preloadJsonSchema() must not swallow it; the exception should bubble up."
        }
      ]
    },
    {
      "group": "Preload with unmodifiable tupleSchema list",
      "cases": [
        {
          "input": [],
          "expected": "No exception thrown; preloadJsonSchemas is called once with the unmodifiable list instance",
          "description": "When tupleSchema is created via Collections.unmodifiableList(...), preloadJsonSchema() should still be able to pass the list to preloadJsonSchemas without attempting to modify it."
        }
      ]
    },
    {
      "group": "Preload preserves order of schemas",
      "cases": [
        {
          "input": [],
          "expected": "No exception thrown; preloadJsonSchemas receives the list in the same order as defined in tupleSchema (e.g., schemaA, schemaB, schemaC)",
          "description": "Ensures that preloadJsonSchema() does not reorder the tupleSchema elements before delegating to preloadJsonSchemas, which is important for positional validation semantics."
        }
      ]
    }
  ]
}