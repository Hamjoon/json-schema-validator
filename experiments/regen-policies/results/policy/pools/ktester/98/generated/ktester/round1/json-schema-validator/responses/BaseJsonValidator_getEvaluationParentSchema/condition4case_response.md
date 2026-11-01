{
  "testCases": [
    {
      "group": "evaluationParentSchemaIsSet",
      "cases": [
        {
          "input": [],
          "expected": "mockEvaluationParentSchema",
          "description": "When the internal field `evaluationParentSchema` is already set (non‑null), `getEvaluationParentSchema()` should return that exact instance without delegating to `getParentSchema()`."
        },
        {
          "input": [],
          "expected": "getParentSchemaNotInvoked",
          "description": "Ensures that `getParentSchema()` is **not** called when `evaluationParentSchema` is present. The test subclass overrides `getParentSchema()` to throw if invoked; the method should complete successfully returning the pre‑set schema."
        }
      ]
    },
    {
      "group": "evaluationParentSchemaIsNull",
      "cases": [
        {
          "input": [],
          "expected": "mockParentSchema",
          "description": "When `evaluationParentSchema` is null, the method must fall back to `getParentSchema()` and return the schema provided by that call."
        },
        {
          "input": [],
          "expected": "getParentSchemaInvokedOnce",
          "description": "Verifies that `getParentSchema()` is invoked exactly once when `evaluationParentSchema` is null. The subclass records the number of invocations and the test asserts the count."
        }
      ]
    }
  ]
}