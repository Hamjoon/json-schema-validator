{
  "testCases": [
    {
      "group": "Empty JSON Object - Validation Enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new SimpleStringOutputFormat<>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "\"Validation succeeded\"",
          "description": "An empty JSON object is supplied. Validation is enabled and a normal OutputFormat is used. The method should deserialize the empty object and return the success message from the OutputFormat."
        }
      ]
    },
    {
      "group": "JSON Literal Null - Validation Enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"null\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new SimpleStringOutputFormat<>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "\"Validation succeeded\"",
          "description": "The input string is the JSON literal `null`. The method should treat it as a valid JSON node (NullNode) and, with validation enabled, return the success message from the OutputFormat."
        }
      ]
    },
    {
      "group": "JSON Array Input - Validation Enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"[1,2,3]\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new SimpleStringOutputFormat<>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "\"Validation succeeded\"",
          "description": "A JSON array is supplied as input. Validation is on and the schema permits arrays (or the test schema is permissive). The method should deserialize the array and return the success message."
        }
      ]
    },
    {
      "group": "Whitespace Only Input",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"   \""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new SimpleStringOutputFormat<>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "JsonProcessingException",
          "description": "The input string contains only whitespace, which is not a valid JSON document. Deserialization should fail with a JsonProcessingException that propagates out of `walk`."
        }
      ]
    },
    {
      "group": "Null Input String",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "null"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new SimpleStringOutputFormat<>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a `null` input string should cause the internal `deserialize` method to throw a NullPointerException (or IllegalArgumentException). The test expects the exception to be propagated."
        }
      ]
    },
    {
      "group": "Null OutputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"key\\\":\\\"value\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "null"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "NullPointerException",
          "description": "If the `outputFormat` argument is `null`, the method will eventually try to invoke it and should throw a NullPointerException. The test verifies that this exception is not swallowed."
        }
      ]
    },
    {
      "group": "OutputFormat Returns Null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"Bob\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new NullReturningOutputFormat<>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "null",
          "description": "A custom OutputFormat implementation that simply returns `null` is supplied. The method should propagate this `null` result without alteration."
        }
      ]
    },
    {
      "group": "ExecutionContext with Customizer (No Effect)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"id\\\":123}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new SimpleStringOutputFormat<>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "\"Validation succeeded\"",
          "description": "Even though the library provides overloads that accept an ExecutionContextCustomizer, the five‑parameter `walk` method does not use one. This test ensures that providing a fully populated ExecutionContext (e.g., with a custom schema version) does not change the basic success path."
        }
      ]
    }
  ]
}