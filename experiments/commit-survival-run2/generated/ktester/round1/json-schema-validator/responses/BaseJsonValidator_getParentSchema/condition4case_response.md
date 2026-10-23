{
  "group": "Parent Schema Retrieval",
  "cases": [
    {
      "input": [],
      "expected": "the same non‑null JsonSchema instance that was injected into the validator",
      "description": "When the validator's internal parentSchema field is set to a concrete JsonSchema object, getParentSchema() should return that exact instance."
    },
    {
      "input": [],
      "expected": "null",
      "description": "When the validator's internal parentSchema field has not been set (remains null), getParentSchema() should return null."
    }
  ]
}