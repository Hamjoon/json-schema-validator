{
  "newTestCases": [
    {
      "group": "Empty and whitespace inputs",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"\""
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
              "value": "null"
            }
          ],
          "expected": "IOException",
          "description": "An empty string is supplied with JSON InputFormat. Deserialization cannot produce a JsonNode and throws an IOException, which should propagate to the caller."
        },
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"   \""
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
              "value": "null"
            }
          ],
          "expected": "IOException",
          "description": "A string containing only whitespace is supplied. The JSON parser treats it as invalid JSON and throws an IOException."
        }
      ]
    },
    {
      "group": "Valid URL‑ENCODED input",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"%7B%22name%22%3A%22John%22%7D\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.URL_ENCODED"
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
          "expected": "\"{\\\"valid\\\":true}\"",
          "description": "A correctly URL‑encoded JSON string is supplied. After decoding it becomes {\"name\":\"John\"}, which validates successfully. The StringOutputFormat returns a JSON string indicating a valid result."
        }
      ]
    },
    {
      "group": "Different InputFormat – YAML",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"name: John\\nage: 30\""
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
          "expected": "\"{\\\"valid\\\":true}\"",
          "description": "The schema supports YAML input. The supplied YAML document is syntactically correct and satisfies the schema, so validation succeeds."
        }
      ]
    },
    {
      "group": "Different OutputFormat – JsonNodeOutputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"active\\\":true}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new JsonNodeOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "JsonNode { valid:true, messages:[] }",
          "description": "Using an OutputFormat that returns a JsonNode. The method should deserialize the JSON, run validation and return a JsonNode representing a successful validation result."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer disables type checking",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"age\\\":\\\"thirty\\\"}\""
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
              "value": "customizerDisablesTypeCheck"
            }
          ],
          "expected": "\"{\\\"valid\\\":true}\"",
          "description": "The schema expects the field 'age' to be an integer. The customizer mutates the ExecutionContext to disable type‑checking validators. Consequently the mismatched type does not cause a validation error and the result is reported as valid."
        }
      ]
    },
    {
      "group": "Large JSON payload",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"[\" + (\"{\\\"id\\\":1,\\\"value\\\":\\\"x\\\"},\".repeat(1000)) + \"]\""
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
              "value": "null"
            }
          ],
          "expected": "\"{\\\"valid\\\":true}\"",
          "description": "A large JSON array containing 1000 objects is validated. The method should handle the size without throwing OutOfMemoryError or other exceptions and return a successful validation result."
        }
      ]
    },
    {
      "group": "Input with UTF‑8 BOM handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"\\uFEFF{\\\"name\\\":\\\"BOM\\\"}\""
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
              "value": "null"
            }
          ],
          "expected": "\"{\\\"valid\\\":true}\"",
          "description": "The input string starts with a UTF‑8 Byte Order Mark. The deserialization logic should ignore the BOM and correctly parse the JSON, resulting in a valid validation outcome."
        }
      ]
    }
  ]
}