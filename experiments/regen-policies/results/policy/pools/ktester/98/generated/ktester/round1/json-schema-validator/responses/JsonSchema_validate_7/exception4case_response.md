{
  "test_cases": [
    {
      "group": "Schema parsing errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing schema {\"type\":\"object\",\"unknownKeyword\":true}"
            },
            {
              "parameter": "format",
              "value": "SimpleOutputFormat instance"
            }
          ],
          "expected": "JsonSchemaException with message containing 'unknown keyword'",
          "description": "When the schema contains an unknown keyword the validator should abort parsing and throw a JsonSchemaException."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing schema {\"$ref\":\"ht!tp://[invalid]\"}"
            },
            {
              "parameter": "format",
              "value": "SimpleOutputFormat instance"
            }
          ],
          "expected": "IllegalArgumentException with message containing 'Invalid $ref' or 'URI'",
          "description": "A malformed $ref URI cannot be resolved; the validator must detect this and throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Unsupported encoding in $id",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing schema {\"$id\":\"http://example.com/schema%ZZ\"}"
            },
            {
              "parameter": "format",
              "value": "SimpleOutputFormat instance"
            }
          ],
          "expected": "UnsupportedEncodingException",
          "description": "If the $id contains an invalid percent‑encoding the validator attempts to decode it and throws UnsupportedEncodingException."
        }
      ]
    },
    {
      "group": "ExecutionContext state validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext(null)" 
            },
            {
              "parameter": "node",
              "value": "valid JsonNode representing a simple schema {\"type\":\"string\"}"
            },
            {
              "parameter": "format",
              "value": "SimpleOutputFormat instance"
            }
          ],
          "expected": "IllegalStateException with message containing 'ValidationContext' or 'must be initialized'",
          "description": "When the ExecutionContext is created without an underlying ValidationContext the validator cannot operate and should throw IllegalStateException."
        }
      ]
    },
    {
      "group": "Circular $ref detection without $recursiveAnchor",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing schema {\"$id\":\"#/\",\"$ref\":\"#/\"}"
            },
            {
              "parameter": "format",
              "value": "SimpleOutputFormat instance"
            }
          ],
          "expected": "JsonSchemaException with message containing 'circular reference'",
          "description": "A schema that references itself via $ref without being marked as a $recursiveAnchor should be detected as a circular reference and cause a JsonSchemaException."
        }
      ]
    }
  ]
}