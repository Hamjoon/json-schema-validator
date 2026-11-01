{
  "tests": [
    {
      "group": "Missing required property – validation error",
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
              "value": "null"
            }
          ],
          "expected": "Set containing a ValidationMessage whose message includes \"required\" – the instance JSON is empty, the schema requires property 'id', so validation fails.",
          "description": "Validates that when the instance JSON does not contain a required property, the method returns a non‑empty ValidationMessage set indicating the missing required field."
        }
      ]
    },
    {
      "group": "Type mismatch – validation error",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}"
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
          "expected": "Set containing a ValidationMessage whose message includes \"type\" or \"integer\" – the property 'age' is a string (default value) while the schema expects an integer, causing a type validation failure.",
          "description": "Ensures that a type mismatch between the instance value and the schema definition is reported as a validation error."
        }
      ]
    },
    {
      "group": "Empty input string – deserialization error",
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
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set containing a ValidationMessage with text mentioning \"empty\" or \"no content\" – the deserializer cannot parse an empty string and reports a parsing problem.",
          "description": "Covers the branch where the provided input string is empty; the method should treat this as a deserialization failure and return an appropriate ValidationMessage."
        }
      ]
    },
    {
      "group": "Malformed YAML input – deserialization error",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "type: object\nproperties:\n  name: {type: string"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Set containing a ValidationMessage whose message contains \"YAML\" or \"parsing\" – the YAML is syntactically incorrect, causing deserialization to fail.",
          "description": "Triggers the YAML deserialization path with malformed YAML to verify that parsing errors are captured and reported as ValidationMessages."
        }
      ]
    },
    {
      "group": "Null InputFormat with customizer – customizer execution",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\",\"properties\":{\"flag\":{\"type\":\"boolean\"}}}"
            },
            {
              "parameter": "inputFormat",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "customizerAddsFlag"
            }
          ],
          "expected": "Set<> (empty) – with a null InputFormat the method defaults to JSON parsing, the customizer runs (adds a flag to the ExecutionContext), and validation succeeds.",
          "description": "Verifies that when InputFormat is null the method falls back to JSON deserialization *and* still invokes the provided ExecutionContextCustomizer."
        }
      ]
    }
  ]
}