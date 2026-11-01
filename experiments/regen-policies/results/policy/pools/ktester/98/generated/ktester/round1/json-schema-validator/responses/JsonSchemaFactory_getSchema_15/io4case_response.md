{
  "tests": [
    {
      "group": "Successful schema loading (additional cases)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"{}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "Returns a non‑null JsonSchema instance representing an empty object schema.",
          "description": "An empty JSON object string should be parsed without error even when the configuration argument is null; the factory must create a default SchemaValidatorsConfig internally."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"{ \\\"type\\\": \\\"array\\\", \\\"items\\\": { \\\"type\\\": \\\"integer\\\" } }\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Returns a non‑null JsonSchema instance whose root node is an array schema with integer items.",
          "description": "Valid JSON schema defining an array type should be loaded correctly; this verifies handling of non‑object root schemas."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"type: array\\nitems:\\n  type: string\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Returns a non‑null JsonSchema instance whose internal JsonNode reflects the YAML array schema converted to JSON.",
          "description": "Valid YAML schema that defines an array should be parsed correctly when a non‑null configuration object is supplied."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"{ \\\"type\\\": \\\"object\\\", \\\"properties\\\": { \\\"id\\\": { \\\"type\\\": \\\"string\\\" } }, \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema#\\\" }\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "Returns a non‑null JsonSchema instance; the presence of a $schema meta‑schema identifier does not prevent loading.",
          "description": "A JSON schema that includes a $schema keyword referencing a known meta‑schema should still be loaded successfully, even when the config argument is null."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"{ \\\"type\\\": \\\"object\\\", \\\"properties\\\": { \\\"value\\\": { \\\"type\\\": \\\"number\\\" } } }\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setFailFast(true)"
            }
          ],
          "expected": "Returns a non‑null JsonSchema instance; the configuration flag (e.g., fail‑fast) is stored inside the created JsonSchema.",
          "description": "Ensures that a non‑default configuration object is passed through to the resulting JsonSchema without causing errors."
        }
      ]
    },
    {
      "group": "Exception handling (additional cases)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws JsonSchemaException wrapping an IOException (or JsonParseException) because the empty string is not valid JSON.",
          "description": "An empty schema string cannot be parsed; the method must convert the low‑level parsing exception into a JsonSchemaException."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"   \\n   \""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws JsonSchemaException wrapping an IOException (or JsonParseException) because whitespace‑only content is not valid JSON.",
          "description": "Whitespace‑only input should be treated as invalid JSON and result in a JsonSchemaException."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"type: object\\nproperties:\\n  name: { type: string }\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws JsonSchemaException wrapping an IOException because the content is YAML while the declared InputFormat is JSON.",
          "description": "Mismatched format (YAML content supplied with InputFormat.JSON) should cause a parsing failure that is wrapped in JsonSchemaException."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "null"
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
          "expected": "Throws JsonSchemaException (likely caused by NullPointerException) because both schema and inputFormat are null.",
          "description": "When all arguments are null the method cannot proceed; it must still convert the resulting low‑level exception into a JsonSchemaException."
        }
      ]
    }
  ]
}