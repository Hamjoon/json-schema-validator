{
  "testCases": [
    {
      "group": "evaluationParentSchema null and parent schema null",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "When the protected field `evaluationParentSchema` is null **and** the overridden `getParentSchema()` returns null, `getEvaluationParentSchema()` should return null rather than throwing an exception."
        }
      ]
    },
    {
      "group": "evaluationParentSchema non‑null prevents parent schema call",
      "cases": [
        {
          "input": [],
          "expected": "returns the JsonSchema instance set as evaluationParentSchema (MockSchemaC) and does NOT invoke getParentSchema()",
          "description": "Even if `getParentSchema()` would return a different schema (MockSchemaD), the method must short‑circuit and return the non‑null `evaluationParentSchema`. A spy on the subclass verifies that `getParentSchema()` is never called."
        }
      ]
    },
    {
      "group": "evaluationParentSchema set after construction",
      "cases": [
        {
          "input": [],
          "expected": "returns the JsonSchema instance that was later assigned to evaluationParentSchema (MockSchemaE)",
          "description": "The validator is instantiated with `evaluationParentSchema` initially null. After construction the field is set via a protected setter (or reflection). Subsequent call to `getEvaluationParentSchema()` must return the newly assigned schema, demonstrating that the method always reads the current field value."
        }
      ]
    }
  ]
}