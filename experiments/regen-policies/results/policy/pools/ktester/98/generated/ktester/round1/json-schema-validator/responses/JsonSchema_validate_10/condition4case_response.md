Invalid control character at: line 18 column 39 (char 454)
{
  "testCases": [
    {
      "group": "Valid inputs – JSON format",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {} // no‑op"
            }
          ],
          "expected": "Set<ValidationMessage> is empty (no validation errors)",
          "description": "A well‑formed JSON document should be deserialized successfully and pass all validators, resulting in an empty validation message set."
        },
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"Anna\",\"age\":25,\"extra\":\"value\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {} // no‑op"
            }
          ],
          "expected": "Set<ValidationMessage> is empty (no validation errors)",
          "description": "Even when the JSON contains additional properties not defined in the schema, the default validator configuration (allowAdditionalProperties = true) should not raise errors."
        }
      ]
    },
    {
      "group": "Valid inputs – YAML format",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "name: \"Bob\"\nage: 40"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {} // no‑op"
            }
          ],
          "expected": "Set<ValidationMessage> is empty (no validation errors)",
          "description": "A well‑formed YAML document should be converted to a JsonNode and validated without errors."
        }
      ]
    },
    {
      "group": "Invalid JSON syntax",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\", \"age\":}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {} // no‑op"
            }
          ],
          "expected": "Set<ValidationMessage> contains a parsing error message (e.g., \"Unexpected token\" or \"Invalid JSON\")",
          "description": "Malformed JSON should cause the deserialization step to fail, producing a validation message describing the parse error."
        }
      ]
    },
    {
      "group": "Invalid YAML syntax",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "name: \"Alice\"\nage: [30,]"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {} // no‑op"
            }
          ],
          "expected": "Set<ValidationMessage> contains a parsing error message (e.g., \"while parsing a flow sequence\" )",
          "description": "Malformed YAML should be caught during deserialization and reported as a validation message."
        }
      ]
    },
    {
      "group": "Null or empty input handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": null
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {} // no‑op"
            }
          ],
          "expected": "IllegalArgumentException is thrown (or ValidationException) indicating that input must not be null",
          "description": "The method should guard against a null string before attempting deserialization."
        },
        {
          "input": [
            {
              "parameter": "input",
              "value": ""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {} // no‑op"
            }
          ],
          "expected": "Set<ValidationMessage> contains a parsing error (empty input cannot be parsed as JSON)",
          "description": "An empty string is not a valid JSON document; the validator should return a parsing‑error message."
        }
      ]
    },
    {
      "group": "UnsupportedEncodingException path",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "%E4%BD%A0%E5%A5%BD"  ,  "note": "URL‑encoded UTF‑8 string"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {} // no‑op"
            }
          ],
          "expected": "Set<ValidationMessage> contains a parsing error if URLDecoder fails, otherwise validation proceeds",
          "description": "When the implementation uses URLDecoder.decode, an unsupported charset would raise UnsupportedEncodingException; this case ensures the method handles it gracefully (e.g., by wrapping in a ValidationMessage)."
        }
      ]
    },
    {
      "group": "Consumer (executionCustomizer) side‑effects",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"Carol\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> executionContext.setAttribute(\"testKey\",\"testValue\")"
            }
          ],
          "expected": "Set<ValidationMessage> is empty and executionContext contains attribute testKey=\"testValue\"",
          "description": "The supplied consumer must be invoked before validation starts, allowing it to modify the ExecutionContext."
        },
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"Dave\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> { throw new RuntimeException(\"customizer failure\"); }"
            }
          ],
          "expected": "RuntimeException propagates out of validate method",
          "description": "If the consumer throws an unchecked exception, the validate method should not swallow it; the test verifies propagation."
        }
      ]
    }
  ]
}