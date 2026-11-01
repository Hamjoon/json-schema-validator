{
  "testCases": [
    {
      "group": "validate_validJson_noCustomizer",
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
              "value": "consumer -> {}"
            }
          ],
          "expected": "empty Set (no validation errors)",
          "description": "A well‑formed JSON document that satisfies the loaded schema should be deserialized correctly and return an empty set of ValidationMessage when no execution customizer is supplied."
        }
      ]
    },
    {
      "group": "validate_invalidJson_syntaxError",
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
              "parameter": "format",
              "value": "OutputFormat<Set<ValidationMessage>>.identity()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "JsonProcessingException",
          "description": "Malformed JSON should cause the internal deserialization step to throw a JsonProcessingException, which propagates out of the validate method."
        }
      ]
    },
    {
      "group": "validate_validYaml_noCustomizer",
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
              "value": "OutputFormat<Set<ValidationMessage>>.identity()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "empty Set (no validation errors)",
          "description": "A well‑formed YAML document that satisfies the schema should be deserialized via YamlMapperFactory and return an empty validation result when no customizer is used."
        }
      ]
    },
    {
      "group": "validate_schemaViolation_requiredProperty",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"age\":30}"
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
              "value": "consumer -> {}"
            }
          ],
          "expected": "Set containing a ValidationMessage with type='required' and message mentioning property 'name'",
          "description": "The JSON document is syntactically correct but violates the schema (missing required property 'name'). The validator must return a non‑empty set describing the required‑property error."
        }
      ]
    },
    {
      "group": "validate_nullInput_throwsNPE",
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
              "value": "OutputFormat<Set<ValidationMessage>>.identity()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null string to the validate method should result in a NullPointerException during deserialization."
        }
      ]
    },
    {
      "group": "validate_withExecutionCustomizer_modifiesContext",
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
              "value": "OutputFormat<ExecutionContext>.extractContext()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"customFlag\", Boolean.TRUE)"
            }
          ],
          "expected": "ExecutionContext where attribute 'customFlag' == Boolean.TRUE",
          "description": "The consumer customizer should be invoked before validation; it adds a custom attribute to the ExecutionContext. The OutputFormat extracts the context, enabling the test to verify that the attribute was set."
        }
      ]
    },
    {
      "group": "validate_outputFormatThrowsException",
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
              "value": "OutputFormat<T>.throwing()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> {}"
            }
          ],
          "expected": "RuntimeException with message 'OutputFormat failure'",
          "description": "If the supplied OutputFormat implementation throws an exception while processing the validation messages, the exception must propagate out of the validate method."
        }
      ]
    }
  ]
}