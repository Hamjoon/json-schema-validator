{
  "testCases": [
    {
      "group": "validate_nullInputFormat_throwsNPE",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "inputFormat",
              "value": null
            },
            {
              "parameter": "format",
              "value": "OutputFormat<Set<ValidationMessage>>.identity()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null InputFormat should cause a NullPointerException when the method tries to deserialize the input."
        }
      ]
    },
    {
      "group": "validate_nullOutputFormat_throwsNPE",
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
              "value": null
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "NullPointerException",
          "description": "Supplying a null OutputFormat leads to a NullPointerException when the validator attempts to apply the format to the validation result."
        }
      ]
    },
    {
      "group": "validate_nullExecutionCustomizer_throwsNPE",
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
              "value": "OutputFormat<Set<ValidationMessage>>.identity()"
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "When the executionCustomizer argument is null, the lambda that invokes accept on it throws a NullPointerException."
        }
      ]
    },
    {
      "group": "validate_mismatchedFormat_throwsJsonProcessingException",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "format",
              "value": "OutputFormat<Set<ValidationMessage>>.identity()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "JsonProcessingException",
          "description": "Providing JSON content while requesting YAML deserialization should cause the internal deserializer to throw a JsonProcessingException."
        }
      ]
    },
    {
      "group": "validate_executionCustomizerThrowsRuntimeException",
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
              "value": "OutputFormat<Set<ValidationMessage>>.identity()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> { throw new IllegalStateException(\"Customizer failure\"); }"
            }
          ],
          "expected": "IllegalStateException",
          "description": "If the supplied executionCustomizer throws a RuntimeException, the exception must propagate out of the validate method unchanged."
        }
      ]
    }
  ]
}