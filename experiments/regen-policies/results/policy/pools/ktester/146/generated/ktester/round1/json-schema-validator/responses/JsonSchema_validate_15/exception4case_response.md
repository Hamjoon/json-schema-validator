{
  "test_cases": [
    {
      "group": "OutputFormat throws RuntimeException",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"John\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new ThrowingOutputFormat(new RuntimeException(\"format failure\"))"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "RuntimeException",
          "description": "A valid JSON string is supplied but the provided OutputFormat throws a RuntimeException during formatting. The validate method must let the exception propagate."
        }
      ]
    },
    {
      "group": "OutputFormat throws IOException",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"id\\\":1}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new ThrowingOutputFormat(new java.io.IOException(\"IO failure\"))"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IOException",
          "description": "A valid JSON string is supplied but the OutputFormat implementation throws a checked IOException. The validate method should propagate the IOException to the caller."
        }
      ]
    },
    {
      "group": "Malformed YAML input",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"name: John\\n:bad\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "format",
              "value": "new StringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IOException",
          "description": "The supplied string is not valid YAML. Deserialization should fail and throw an IOException, which must be propagated."
        }
      ]
    },
    {
      "group": "Unsupported InputFormat handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"key\\\":\\\"value\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.XML"
            },
            {
              "parameter": "format",
              "value": "new StringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "InputFormat.XML is not supported by the JsonSchema implementation. Passing it should cause the method to throw an IllegalArgumentException before any deserialization attempt."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer returns null context",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"type\\\":\\\"string\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new StringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "customizerReturnsNull"
            }
          ],
          "expected": "NullPointerException",
          "description": "The customizer mutates the ExecutionContext and returns null. Subsequent use of the context inside validation should result in a NullPointerException, which must propagate."
        }
      ]
    }
  ]
}