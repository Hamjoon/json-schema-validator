{
  "testCases": [
    {
      "group": "validateValidJsonString",
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
              "parameter": "format",
              "value": "OutputFormat.SET"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "A well‑formed JSON document that conforms to the schema should result in an empty set of validation messages when using the default OutputFormat that returns a Set."
        }
      ]
    },
    {
      "group": "validateInvalidJsonString",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\",\"age\":\"thirty\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "OutputFormat.SET"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "non‑empty Set<ValidationMessage>",
          "description": "A JSON document that violates the schema (age is a string instead of a number) should produce a non‑empty set of validation messages."
        }
      ]
    },
    {
      "group": "validateEmptyString",
      "cases": [
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
              "parameter": "format",
              "value": "OutputFormat.SET"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "JsonProcessingException",
          "description": "An empty string cannot be parsed as JSON and should cause a JsonProcessingException to be thrown by the deserialize step."
        }
      ]
    },
    {
      "group": "validateNullInput",
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
              "parameter": "format",
              "value": "OutputFormat.SET"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null input string should result in a NullPointerException before any validation occurs."
        }
      ]
    },
    {
      "group": "validateUnsupportedInputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.UNSUPPORTED"
            },
            {
              "parameter": "format",
              "value": "OutputFormat.SET"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Supplying an InputFormat that the implementation does not support should cause an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "validateWithCustomizerModifiesContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "OutputFormat.SET"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> consumer.setAttribute(\"custom\",true)"
            }
          ],
          "expected": "empty Set<ValidationMessage> and ExecutionContext attribute 'custom' set to true",
          "description": "The consumer should be invoked and be able to mutate the ExecutionContext (e.g., setting a custom attribute). Validation should still succeed and return an empty set."
        }
      ]
    },
    {
      "group": "validateCustomizerThrowsRuntimeException",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "OutputFormat.SET"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> { throw new RuntimeException(\"custom failure\"); }"
            }
          ],
          "expected": "RuntimeException with message 'custom failure'",
          "description": "If the consumer throws a RuntimeException, the exception should propagate out of the validate method."
        }
      ]
    },
    {
      "group": "validateYamlInputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "name: John\nage: 30"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "format",
              "value": "OutputFormat.SET"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "empty Set<ValidationMessage>",
          "description": "A valid YAML document should be correctly deserialized and validated, resulting in an empty set of validation messages."
        }
      ]
    }
  ]
}