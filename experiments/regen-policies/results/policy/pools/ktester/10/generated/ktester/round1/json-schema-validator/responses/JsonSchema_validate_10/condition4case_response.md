{
  "testCases": [
    {
      "group": "Valid JSON input – no customizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<> (empty) – the JSON is valid against the schema, no ValidationMessage objects are returned.",
          "description": "A well‑formed JSON document with the default JSON InputFormat and a null ExecutionContextCustomizer should be deserialized successfully and pass validation, exercising the happy‑path branch where no exception is thrown and no customizer is applied."
        }
      ]
    },
    {
      "group": "Valid YAML input – with customizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "type: object\nproperties:\n  age:\n    type: integer"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "executionCustomizer",
              "value": "customizerAddsFlag"
            }
          ],
          "expected": "Set<> (empty) – the YAML is correctly deserialized, the customizer runs (adds a flag to the ExecutionContext), and validation succeeds.",
          "description": "Provides a YAML document to trigger the branch that selects the YAML deserializer. The supplied ExecutionContextCustomizer (represented by the placeholder `customizerAddsFlag`) is invoked, allowing verification that the customizer hook is exercised without affecting validation outcome."
        }
      ]
    },
    {
      "group": "Null input string",
      "cases": [
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
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException – the method should reject a null input string before attempting deserialization.",
          "description": "Ensures the branch that checks for a null `input` argument is covered, expecting an IllegalArgumentException to be thrown."
        }
      ]
    },
    {
      "group": "Unsupported/invalid JSON – deserialization error",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{invalidJson: true unquoted}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set containing a ValidationMessage with message containing 'invalid JSON' – the deserialization step throws JsonProcessingException which is caught and turned into a validation error.",
          "description": "Triggers the branch where `deserialize` fails (malformed JSON). The method should capture the exception and return a ValidationMessage describing the parsing problem."
        }
      ]
    },
    {
      "group": "Null InputFormat – fallback to JSON",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set<> (empty) – when InputFormat is null, the implementation defaults to JSON deserialization and validation succeeds.",
          "description": "Covers the branch that handles a null `inputFormat` argument, verifying that the method defaults to JSON parsing and proceeds without error."
        }
      ]
    },
    {
      "group": "Custom ExecutionContextCustomizer that disables a validator",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}},\"required\":[\"id\"]}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "customizerDisablesRequiredValidator"
            }
          ],
          "expected": "Set<> (empty) – the customizer disables the required‑field validator, so the missing required property does not produce a ValidationMessage.",
          "description": "Tests the branch where the supplied `ExecutionContextCustomizer` mutates the ExecutionContext (e.g., disables the required validator). The validation result should reflect the customized context."
        }
      ]
    }
  ]
}