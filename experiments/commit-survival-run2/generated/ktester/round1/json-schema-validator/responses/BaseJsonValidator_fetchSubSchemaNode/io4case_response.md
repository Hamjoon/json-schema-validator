{
  "testCases": [
    {
      "group": "fetchSubSchemaNode - schemaNode is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mocked ValidationContext (base URI = \"http://example.com/schema\")"
            }
          ],
          "expected": "null",
          "description": "When suppressSubSchemaRetrieval is false but the validator's schemaNode field is null, fetchSubSchemaNode should call obtainSubSchemaNode with a null schemaNode and return whatever that method returns – in the current implementation this is null."
        },
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "null"
            }
          ],
          "expected": "null",
          "description": "Even when the supplied ValidationContext is null, a null schemaNode still leads obtainSubSchemaNode to return null, so fetchSubSchemaNode must return null."
        }
      ]
    },
    {
      "group": "fetchSubSchemaNode - schemaNode contains $ref",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mocked ValidationContext with base URI \"http://example.com/schema\""
            }
          ],
          "expected": "non‑null JsonSchema whose id is \"http://example.com/schema#/definitions/foo\"",
          "description": "When the validator holds a schemaNode that is an ObjectNode with a $ref (e.g. {\"$ref\":\"#/definitions/foo\"}) and suppressSubSchemaRetrieval is false, fetchSubSchemaNode must delegate to obtainSubSchemaNode, which resolves the reference against the ValidationContext's base URI. The returned JsonSchema should therefore represent the resolved definition."
        },
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "null"
            }
          ],
          "expected": "non‑null JsonSchema (reference resolved relative to the schemaNode's own URI, if any)",
          "description": "If the ValidationContext is null, obtainSubSchemaNode should still be able to resolve a $ref using the schemaNode's internal $id (or default to empty). The test verifies that fetchSubSchemaNode does not throw and returns a JsonSchema instance."
        }
      ]
    },
    {
      "group": "fetchSubSchemaNode - validationContext with custom base URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mocked ValidationContext with base URI \"http://other.com/api\""
            }
          ],
          "expected": "non‑null JsonSchema whose base URI is \"http://other.com/api\"",
          "description": "When a ValidationContext with a different base URI is supplied, fetchSubSchemaNode must forward it to obtainSubSchemaNode. The resulting JsonSchema should carry the supplied base URI, confirming that the method does not alter the context."
        },
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mocked ValidationContext with base URI \"file:/tmp/schema.json\""
            }
          ],
          "expected": "non‑null JsonSchema whose base URI is \"file:/tmp/schema.json\"",
          "description": "Ensures that file‑scheme base URIs are also propagated correctly through fetchSubSchemaNode."
        }
      ]
    },
    {
      "group": "fetchSubSchemaNode - suppressSubSchemaRetrieval = false, but obtainSubSchemaNode throws",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mocked ValidationContext that causes obtainSubSchemaNode to throw IllegalArgumentException"
            }
          ],
          "expected": "IllegalArgumentException propagated",
          "description": "If obtainSubSchemaNode throws an unchecked exception (e.g., because the schemaNode is malformed), fetchSubSchemaNode must not catch it; the exception should propagate to the caller."
        }
      ]
    }
  ]
}