{
  "testCases": [
    {
      "group": "evaluationParentSchema is non‑null",
      "cases": [
        {
          "input": [],
          "expected": "returns the JsonSchema instance that was set as evaluationParentSchema (MockSchemaA)",
          "description": "When the protected field `evaluationParentSchema` is assigned a non‑null JsonSchema, `getEvaluationParentSchema()` must return that exact instance without delegating to `getParentSchema()`."
        }
      ]
    },
    {
      "group": "evaluationParentSchema is null",
      "cases": [
        {
          "input": [],
          "expected": "returns the JsonSchema instance provided by getParentSchema() (MockSchemaB)",
          "description": "When `evaluationParentSchema` is null, the method should fall back to `getParentSchema()`. The test subclass overrides `getParentSchema()` to return a known mock schema and verifies that this value is returned."
        }
      ]
    }
  ]
}