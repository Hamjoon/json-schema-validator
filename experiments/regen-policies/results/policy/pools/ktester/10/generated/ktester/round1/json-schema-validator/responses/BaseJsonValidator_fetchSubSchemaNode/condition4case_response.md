{
  "group": "fetchSubSchemaNode - suppressSubSchemaRetrieval flag",
  "cases": [
    {
      "input": [
        {
          "parameter": "validationContext",
          "value": "new ValidationContext.Builder().build()"
        }
      ],
      "expected": "null",
      "description": "When `suppressSubSchemaRetrieval` is true the method should short‑circuit and return `null` regardless of the supplied `ValidationContext` or the content of `schemaNode`."
    },
    {
      "input": [
        {
          "parameter": "validationContext",
          "value": "new ValidationContext.Builder().schemaFactory(new JsonSchemaFactory()).build()"
        }
      ],
      "expected": "instance of JsonSchema (non‑null)",
      "description": "When `suppressSubSchemaRetrieval` is false the method must delegate to `obtainSubSchemaNode`. With a minimal valid schema (`{\"type\":\"string\"}`) the delegate creates a `JsonSchema` object, therefore the returned value must be a non‑null `JsonSchema` instance."
    },
    {
      "input": [
        {
          "parameter": "validationContext",
          "value": "null"
        }
      ],
      "expected": "NullPointerException",
      "description": "If a `null` `ValidationContext` is supplied while `suppressSubSchemaRetrieval` is false, the private helper `obtainSubSchemaNode` will be invoked with a `null` argument and is expected to throw a `NullPointerException`. The test verifies that the method does not swallow the exception."
    }
  ]
}