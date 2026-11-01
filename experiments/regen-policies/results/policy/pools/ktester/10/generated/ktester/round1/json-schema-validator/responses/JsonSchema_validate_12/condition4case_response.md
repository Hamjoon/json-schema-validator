Invalid control character at: line 18 column 49 (char 487)
{
  "testCases": [
    {
      "group": "Valid inputs – successful validation and customizer execution",
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
              "value": "new StringOutputFormat() // returns the input string unchanged"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"custom\", true)" 
            }
          ],
          "expected": "\"{\\\"name\\\":\\\"John\\\"}\"",
          "description": "A well‑formed JSON string is deserialized, validated and the supplied OutputFormat returns the original JSON string. The executionCustomizer must be invoked – we verify that the custom attribute is set on the ExecutionContext."
        },
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
              "value": "new JsonNodeOutputFormat() // returns a JsonNode representation"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.addMessage(\"customizer‑executed\")"
            }
          ],
          "expected": "JsonNode representing {\"name\":\"John\",\"age\":30}",
          "description": "A well‑formed YAML string is deserialized to a JsonNode, validated and returned via a JsonNode‑based OutputFormat. The customizer must be called – we check that the custom message is present in the ExecutionContext."
        }
      ]
    },
    {
      "group": "Invalid inputs – exception handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{ invalid json }\""
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
              "value": "ctx -> {}"
            }
          ],
          "expected": "JsonProcessingException",
          "description": "Malformed JSON triggers a deserialization failure. The method should propagate a JsonProcessingException (or a wrapped RuntimeException depending on implementation). No customizer execution is expected."
        },
        {
          "input": [
            {
              "parameter": "input",
              "value": "null"
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
              "value": "ctx -> {}"
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null input string should cause a NullPointerException before deserialization."
        },
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"John\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "null"
            },
            {
              "parameter": "format",
              "value": "new StringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "NullPointerException",
          "description": "A null InputFormat cannot be used for deserialization and should result in a NullPointerException."
        },
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
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "NullPointerException",
          "description": "A null OutputFormat is illegal; the method should throw a NullPointerException before invoking the internal validator."
        },
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
              "value": "new StringOutputFormat()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "If the executionCustomizer is null, the lambda `(executionContext, validationContext) -> executionCustomizer.accept(executionContext)` will throw a NullPointerException when invoked."
        }
      ]
    }
  ]
}