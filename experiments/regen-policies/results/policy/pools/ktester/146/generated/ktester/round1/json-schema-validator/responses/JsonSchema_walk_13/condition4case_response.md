{
  "testCases": [
    {
      "group": "Valid Input - Validation Enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"John\\\"}\""
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
          "description": "A well‑formed JSON string is supplied, validation is turned on and a simple OutputFormat that returns a success message is used. The method should deserialize the JSON and invoke walkAtNodeInternal, ultimately returning the string produced by the OutputFormat."
        }
      ]
    },
    {
      "group": "Valid Input - Validation Disabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"age\\\":30}\""
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
              "value": "false"
            }
          ],
          "expected": "\"Validation skipped\"",
          "description": "A valid JSON payload is supplied but the validate flag is false. The walk method should still deserialize the input and call walkAtNodeInternal with validation disabled, returning the string from the OutputFormat that indicates validation was skipped."
        }
      ]
    },
    {
      "group": "Invalid JSON Input",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{invalid json}\""
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
          "description": "The input string is not valid JSON. deserialize() is expected to throw a JsonProcessingException (or a subclass). The walk method should propagate this exception."
        }
      ]
    },
    {
      "group": "Null ExecutionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"id\\\":1}\""
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
          "description": "When a null ExecutionContext is supplied the method should create a default ExecutionContext internally (as per library contract) and continue normal processing, returning the success message from the OutputFormat."
        }
      ]
    },
    {
      "group": "Unsupported InputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"some text\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"  ,
              "note": "Assume the library does not support YAML in this version"
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
          "expected": "UnsupportedOperationException",
          "description": "Passing an InputFormat that the implementation cannot handle should cause deserialize() to throw an UnsupportedOperationException, which the walk method propagates."
        }
      ]
    },
    {
      "group": "OutputFormat Throws Exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"Alice\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new ThrowingOutputFormat<>()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "RuntimeException",
          "description": "The OutputFormat implementation throws a RuntimeException during its `apply` call. walkAtNodeInternal should propagate this exception, so the test expects a RuntimeException."
        }
      ]
    }
  ]
}