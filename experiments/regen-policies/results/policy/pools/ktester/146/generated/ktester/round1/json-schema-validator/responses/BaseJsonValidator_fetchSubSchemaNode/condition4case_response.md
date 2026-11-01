{
  "testCases": [
    {
      "group": "Suppress SubSchema Retrieval",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mockValidationContext"
            }
          ],
          "expected": "null",
          "description": "When the validator is constructed with suppressSubSchemaRetrieval = true, fetchSubSchemaNode should short‑circuit and return null regardless of the supplied ValidationContext."
        },
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "null"
            }
          ],
          "expected": "null",
          "description": "Even if a null ValidationContext is passed, the suppress flag takes precedence and fetchSubSchemaNode must still return null."
        }
      ]
    },
    {
      "group": "Retrieve SubSchema Node",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mockValidationContext"
            }
          ],
          "expected": "mockJsonSchema",
          "description": "When suppressSubSchemaRetrieval = false, fetchSubSchemaNode should delegate to the private static obtainSubSchemaNode method and return its result. The test mocks obtainSubSchemaNode (using PowerMockito/Mockito) to return a dummy JsonSchema instance and verifies that the returned value matches the mock."
        },
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mockValidationContext"
            }
          ],
          "expected": "null",
          "description": "If obtainSubSchemaNode returns null (e.g., because the underlying schemaNode is null or invalid), fetchSubSchemaNode must propagate that null value. The test sets schemaNode to null, mocks obtainSubSchemaNode to return null, and asserts the method returns null."
        }
      ]
    }
  ]
}