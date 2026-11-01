{
  "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig)",
  "cases": [
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" } } }"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "non‑null JsonSchema instance whose meta‑schema is Draft‑04 (the `$schema` keyword is honoured) and that validates an object with an integer `id` correctly",
      "description": "Schema explicitly declares Draft‑04 via `$schema`; the factory must load the corresponding meta‑schema and associate it with the returned JsonSchema."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "true"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "non‑null JsonSchema instance that always validates (i.e., `validate` never reports errors) because the schema is the boolean `true` per the JSON‑Schema spec",
      "description": "A boolean schema `true` is a valid JSON‑Schema that matches any instance; the method should accept it and produce a permissive JsonSchema."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "false"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "non‑null JsonSchema instance that always fails validation (i.e., `validate` always returns an error) because the schema is the boolean `false`",
      "description": "A boolean schema `false` is a valid JSON‑Schema that matches no instance; the factory must return a JsonSchema that rejects everything."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"$ref\": \"http://nonexistent.example.com/schema.json\" }"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "JsonSchemaException (or a subclass) indicating that the external `$ref` could not be resolved",
      "description": "External `$ref` pointing to a non‑existent URI should cause schema loading to fail with a clear exception."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"definitions\": { \"A\": { \"$ref\": \"#/definitions/B\" }, \"B\": { \"$ref\": \"#/definitions/A\" } }, \"$ref\": \"#/definitions/A\" }"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "JsonSchemaException (or a specific cyclic‑reference exception) because the schema contains a cyclic `$ref` that cannot be resolved",
      "description": "Detect and report a cyclic reference within the same document; the factory must not enter an infinite loop."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"type\": \"object\" \"properties\": { \"name\": { \"type\": \"string\" } } }"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "JsonSchemaException (or IOException) caused by malformed JSON (missing comma between fields)",
      "description": "Invalid JSON syntax should be caught during parsing and reported as a schema‑loading exception."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "\uFEFF{ \"type\": \"object\", \"properties\": { \"code\": { \"type\": \"string\" } } }"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "non‑null JsonSchema instance; the leading UTF‑8 BOM is ignored and the schema is parsed successfully",
      "description": "A schema string that starts with a Unicode Byte Order Mark should still be accepted."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"$id\": \"http://example.com/root.json\", \"type\": \"object\", \"definitions\": { \"inner\": { \"type\": \"string\" } }, \"properties\": { \"inner\": { \"$ref\": \"root.json#/definitions/inner\" } } }"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "non‑null JsonSchema instance where the `$ref` resolves to the local definition via the base URI derived from `$id`",
      "description": "Tests proper handling of `$id` and relative `$ref` resolution within the same document."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"anyOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "non‑null JsonSchema instance that validates a value if it is either a string or an integer",
      "description": "Schema using the `anyOf` combinator should be loaded without error; the resulting JsonSchema must correctly apply the combinator semantics."
    }
  ]
}