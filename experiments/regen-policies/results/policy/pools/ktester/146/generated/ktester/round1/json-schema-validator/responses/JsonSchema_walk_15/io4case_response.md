{
  "testCases": [
    {
      "group": "walk_validJson_validateTrue",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "{ \"name\": \"John\", \"age\": 30 }"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "OutputFormat.SET_OF_MESSAGES"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> containing zero messages (valid instance)",
          "description": "A well‑formed JSON document that conforms to the schema is walked with validation enabled. The output format returns a Set of ValidationMessage objects; the set should be empty because the instance is valid."
        }
      ]
    },
    {
      "group": "walk_invalidJson_validateTrue",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "{ \"name\": 123, \"age\": \"thirty\" }"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "OutputFormat.SET_OF_MESSAGES"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> containing at least two messages (type mismatches)",
          "description": "A JSON document that violates the schema (wrong types for 'name' and 'age') is walked with validation enabled. The output format returns a Set of ValidationMessage objects; the set should contain messages describing each violation."
        }
      ]
    },
    {
      "group": "walk_validJson_validateFalse",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "{ \"name\": \"Alice\", \"age\": 25 }"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "OutputFormat.SET_OF_MESSAGES"
            },
            {
              "parameter": "validate",
              "value": "false"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> containing zero messages (no validation performed)",
          "description": "When validation is disabled, the walk method should still deserialize the JSON and invoke the output format, but it must not produce any validation messages regardless of the instance content."
        }
      ]
    },
    {
      "group": "walk_malformedJson_throwsException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "{ \"name\": \"Bob\", \"age\": 40 "   }
            ,
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "OutputFormat.SET_OF_MESSAGES"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "JsonParseException (or IOException) wrapped in ValidationException",
          "description": "A syntactically invalid JSON string should cause the internal deserialize step to fail, resulting in an exception being thrown before any walking or validation occurs."
        }
      ]
    },
    {
      "group": "walk_withExecutionCustomizer_modifiesContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "{ \"name\": \"Carol\", \"age\": 22 }"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "OutputFormat.SET_OF_MESSAGES"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setConfig(new SchemaValidatorsConfig().setFailFast(true))"
            }
          ],
          "expected": "Set<ValidationMessage> containing zero messages (valid) and execution context configured with fail‑fast=true",
          "description": "The customizer lambda modifies the ExecutionContext (e.g., enabling fail‑fast). The walk should honour the customisation; because the instance is valid, the result set is empty and no exception is thrown."
        }
      ]
    },
    {
      "group": "walk_withCustomOutputFormat_returnsCustomObject",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "{ \"name\": \"Dave\", \"age\": 45 }"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<String>() { public String apply(Set<ValidationMessage> msgs) { return msgs.isEmpty()? \"OK\" : \"FAIL\"; } }"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "\"OK\"",
          "description": "A custom OutputFormat that converts the validation result set into a simple string should be invoked. For a valid instance the returned string must be \"OK\"."
        }
      ]
    },
    {
      "group": "walk_nullInput_returnsEmptySet",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultExecutionContext()"
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
              "value": "OutputFormat.SET_OF_MESSAGES"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> containing one message indicating null/empty input",
          "description": "When the input string is null, the deserialize step should treat it as an empty JSON node, leading to a validation message that the root is missing or null."
        }
      ]
    },
    {
      "group": "walk_yamlInputFormat_validatesCorrectly",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "defaultExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "name: \"Eve\"\nage: 28"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "outputFormat",
              "value": "OutputFormat.SET_OF_MESSAGES"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<ValidationMessage> containing zero messages (valid YAML instance)",
          "description": "The method must correctly deserialize YAML when InputFormat.YAML is supplied and perform validation, yielding an empty message set for a conforming document."
        }
      ]
    }
  ]
}