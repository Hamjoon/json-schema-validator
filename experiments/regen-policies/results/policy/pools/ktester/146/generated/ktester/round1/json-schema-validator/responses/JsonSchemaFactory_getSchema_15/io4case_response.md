{
  "testCases": [
    {
      "group": "Null handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "null"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "throws NullPointerException (schema argument is null)",
          "description": "Passing a null schema string should cause an immediate NullPointerException before any parsing is attempted."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"   \\n\\t  \""
            },
            {
              "parameter": "inputFormat",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "throws JsonSchemaException wrapping an IOException (whitespace‑only content cannot be parsed)",
          "description": "A schema consisting only of whitespace is not a valid JSON/YAML document; readTree throws an IOException which is wrapped by JsonSchemaException."
        }
      ]
    },
    {
      "group": "Config null handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{\"type\":\"number\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "returns a non‑null JsonSchema instance (default configuration applied)",
          "description": "When config is null the factory should use its default SchemaValidatorsConfig while still parsing a valid JSON schema."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "type: string\nminLength: 3"
            },
            {
              "parameter": "inputFormat",
              "value": "YAML"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "returns a non‑null JsonSchema instance (default configuration applied)",
          "description": "Valid YAML schema with a null config should be parsed successfully using the factory's default validators."
        }
      ]
    },
    {
      "group": "Mixed format handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}"
            },
            {
              "parameter": "inputFormat",
              "value": "YAML"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns a non‑null JsonSchema instance (YAML parser accepts JSON syntax)",
          "description": "Supplying a JSON document while declaring InputFormat.YAML should still succeed because the YAML mapper can read JSON."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "type: object\nproperties:\n  id:\n    type: integer"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "throws JsonSchemaException wrapping an IOException (YAML content cannot be parsed as JSON)",
          "description": "Providing YAML content with InputFormat.JSON forces the JSON parser to fail, exercising the exception path."
        }
      ]
    },
    {
      "group": "Parsing edge cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{/* comment */\"type\":\"string\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "throws JsonSchemaException wrapping an IOException (JSON with comments is invalid)",
          "description": "JSON with Java‑style comments is not valid; the parser throws an IOException which is wrapped as JsonSchemaException."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},\"additionalProperties\":false}"
            },
            {
              "parameter": "inputFormat",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns a non‑null JsonSchema instance (default format detection works for a complex schema)",
          "description": "A more complex, but still valid, JSON schema with null InputFormat ensures the default format handling correctly parses the content."
        }
      ]
    }
  ]
}