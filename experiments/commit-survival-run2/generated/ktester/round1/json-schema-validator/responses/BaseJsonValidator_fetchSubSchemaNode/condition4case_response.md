{
  "testCases": [
    {
      "group": "fetchSubSchemaNode - suppressSubSchemaRetrieval = true",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mocked ValidationContext"
            }
          ],
          "expected": "null",
          "description": "When the validator's suppressSubSchemaRetrieval flag is true, fetchSubSchemaNode must short‑circuit and return null regardless of the supplied ValidationContext."
        },
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "null"
            }
          ],
          "expected": "null",
          "description": "Even if a null ValidationContext is passed, the suppressSubSchemaRetrieval flag takes precedence and the method must still return null."
        }
      ]
    },
    {
      "group": "fetchSubSchemaNode - suppressSubSchemaRetrieval = false",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mocked ValidationContext with base URI \"http://example.com/schema\""
            }
          ],
          "expected": "non‑null JsonSchema instance (the schema returned by obtainSubSchemaNode)",
          "description": "With suppressSubSchemaRetrieval set to false, the method should delegate to the private static obtainSubSchemaNode, passing the current schemaNode and the provided ValidationContext, and return the resulting JsonSchema."
        },
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "null"
            }
          ],
          "expected": "non‑null JsonSchema instance (the schema returned by obtainSubSchemaNode when validationContext is null)",
          "description": "When suppressSubSchemaRetrieval is false and a null ValidationContext is supplied, fetchSubSchemaNode must still call obtainSubSchemaNode with the current schemaNode and null, returning the JsonSchema produced by that call."
        }
      ]
    }
  ]
}