{
  "group": "hasTypeValidator",
  "cases": [
    {
      "input": [
        {
          "parameter": "jsonSchema",
          "value": "JsonSchema spy where getTypeValidator() is stubbed to return a non‑null mock TypeValidator"
        }
      ],
      "expected": "true",
      "description": "When the schema has a TypeValidator (getTypeValidator() returns a non‑null instance), hasTypeValidator() should return true, exercising the true branch of the conditional."
    },
    {
      "input": [
        {
          "parameter": "jsonSchema",
          "value": "JsonSchema spy where getTypeValidator() is stubbed to return null"
        }
      ],
      "expected": "false",
      "description": "When the schema does not have a TypeValidator (getTypeValidator() returns null), hasTypeValidator() should return false, exercising the false branch of the conditional."
    }
  ]
}