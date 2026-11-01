{
  "test_cases": [
    {
      "group": "ExecutionContextCustomizer Exceptions",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "JsonInputFormat.INSTANCE"
            },
            {
              "parameter": "format",
              "value": "new SimpleOutputFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new IllegalStateException(\"customizer failure\"); }"
            }
          ],
          "expected": "IllegalStateException with message \"customizer failure\"",
          "description": "When the provided ExecutionContextCustomizer throws an unchecked exception, the validate method must propagate that exception without wrapping."
        }
      ]
    },
    {
      "group": "OutputFormat Exceptions",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"age\":30}"
            },
            {
              "parameter": "inputFormat",
              "value": "JsonInputFormat.INSTANCE"
            },
            {
              "parameter": "format",
              "value": "new ThrowingOutputFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "RuntimeException with message \"output format failure\"",
          "description": "If the OutputFormat implementation throws an exception while converting the ValidationResult, the exception should be propagated to the caller."
        }
      ]
    },
    {
      "group": "InputFormat IOException",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "any string"
            },
            {
              "parameter": "inputFormat",
              "value": "new IOExceptionThrowingInputFormat()"
            },
            {
              "parameter": "format",
              "value": "new SimpleOutputFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IOException wrapped in JsonSchemaException",
          "description": "A custom InputFormat that throws a generic IOException during deserialization must have that IOException wrapped inside a JsonSchemaException by the validate method."
        }
      ]
    },
    {
      "group": "Schema Validation Internal Exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"$ref\":\"#/nonexistent\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "JsonInputFormat.INSTANCE"
            },
            {
              "parameter": "format",
              "value": "new SimpleOutputFormat<>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "JsonSchemaException with message containing \"Unable to resolve reference\"",
          "description": "When the schema contains an invalid $ref that cannot be resolved, the validation step throws a JsonSchemaException. This case verifies that such internal schema errors are surfaced correctly."
        }
      ]
    }
  ]
}