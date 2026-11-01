{
  "testCases": [
    {
      "group": "getSchema(String, InputFormat) - null schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "null"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) because the schema string is null",
          "description": "The method must reject a null schema argument and throw an exception rather than returning a JsonSchema."
        }
      ]
    },
    {
      "group": "getSchema(String, InputFormat) - empty schema string",
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
            }
          ],
          "expected": "JsonSchemaException (or IOException) indicating that the content cannot be parsed as JSON",
          "description": "An empty string is not a valid JSON document; the factory should surface a parsing error."
        }
      ]
    },
    {
      "group": "getSchema(String, InputFormat) - invalid JSON content",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"not a json\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "JsonSchemaException (or IOException) caused by malformed JSON",
          "description": "The supplied string is syntactically invalid JSON; the method must fail during parsing."
        }
      ]
    },
    {
      "group": "getSchema(String, InputFormat) - valid YAML schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"type: object\\nproperties:\\n  name:\\n    type: string\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            }
          ],
          "expected": "non‑null JsonSchema instance whose root node type is OBJECT and meta‑schema version matches the default",
          "description": "When the input format is YAML and the content is a well‑formed YAML schema, the factory should correctly parse and return a usable JsonSchema."
        }
      ]
    },
    {
      "group": "getSchema(String, InputFormat) - mismatched format (JSON string as YAML)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"id\\\":{\\\"type\\\":\\\"integer\\\"}}}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            }
          ],
          "expected": "JsonSchemaException (or IOException) because the JSON text cannot be parsed as YAML",
          "description": "The method must not silently accept a JSON document when the caller explicitly requests YAML parsing."
        }
      ]
    },
    {
      "group": "getSchema(String, InputFormat) - null InputFormat with YAML content",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"type: object\\nproperties:\\n  age:\\n    type: integer\""
            },
            {
              "parameter": "inputFormat",
              "value": "null"
            }
          ],
          "expected": "JsonSchemaException (or IOException) because the default JSON parser cannot handle YAML syntax",
          "description": "When inputFormat is null the factory falls back to JSON; providing YAML should therefore cause a parsing failure."
        }
      ]
    },
    {
      "group": "getSchema(String, InputFormat) - schema with $ref to external definition",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"{\\\"$ref\\\": \\\"#/definitions/person\\\", \\\"definitions\\\": {\\\"person\\\": {\\\"type\\\": \\\"object\\\", \\\"properties\\\": {\\\"name\\\": {\\\"type\\\": \\\"string\\\"}}}}}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "non‑null JsonSchema instance; the $ref is resolved to the internal definition without throwing an exception",
          "description": "The factory should correctly handle schemas that contain $ref pointers to definitions within the same document."
        }
      ]
    },
    {
      "group": "getSchema(String, InputFormat) - unknown meta‑schema IRI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"{\\\"$schema\\\": \\\"http://example.com/unknown-draft\\\", \\\"type\\\": \\\"object\\\"}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "non‑null JsonSchema instance that falls back to the default meta‑schema (e.g., Draft‑7)",
          "description": "If the $schema IRI is not recognised, the factory should not fail but use the default meta‑schema."
        }
      ]
    },
    {
      "group": "getSchema(String, InputFormat) - schema with leading UTF‑8 BOM",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"\\uFEFF{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"code\\\":{\\\"type\\\":\\\"string\\\"}}}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "non‑null JsonSchema instance; the BOM is ignored and parsing succeeds",
          "description": "The factory must be tolerant of a Byte Order Mark at the start of the schema string."
        }
      ]
    }
  ]
}