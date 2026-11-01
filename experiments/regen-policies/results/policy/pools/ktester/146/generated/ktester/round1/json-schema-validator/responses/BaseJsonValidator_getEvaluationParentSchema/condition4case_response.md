{
  "group": "EvaluationParentSchema Retrieval",
  "cases": [
    {
      "input": [],
      "expected": "evaluationParentSchemaInstance",
      "description": "evaluationParentSchema field is pre‑populated (non‑null). The method should return this instance without delegating to getParentSchema()."
    },
    {
      "input": [],
      "expected": "parentSchemaInstance",
      "description": "evaluationParentSchema field is null. The method should fall back to getParentSchema() and return the parent schema that has been set on the validator."
    },
    {
      "input": [],
      "expected": "null",
      "description": "Both evaluationParentSchema and the parent schema are null. The method should return null (the fallback path is exercised and getParentSchema() also yields null)."
    }
  ]
}