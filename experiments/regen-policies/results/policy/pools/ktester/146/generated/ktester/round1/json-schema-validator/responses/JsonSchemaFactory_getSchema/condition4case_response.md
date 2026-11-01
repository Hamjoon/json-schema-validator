{
  "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig)",
  "cases": [
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "non‑null JsonSchema instance whose meta‑schema is the default draft‑7 and which validates a simple object correctly",
      "description": "Happy‑path: a well‑formed JSON schema string with a default (empty) configuration should be parsed and a JsonSchema object returned."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }"
        },
        {
          "parameter": "config",
          "value": "SchemaValidatorsConfig.builder().setFailFast(true).build()"
        }
      ],
      "expected": "non‑null JsonSchema instance that respects the supplied config (e.g., fail‑fast mode is enabled)",
      "description": "Verify that a custom `SchemaValidatorsConfig` is propagated to the created schema."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "null"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "IllegalArgumentException (or NullPointerException) indicating that the schema string must not be null",
      "description": "Calling `getSchema` with a `null` schema string should result in an exception rather than a silent failure."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "\"\""
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "IllegalArgumentException stating that the schema string is empty or blank",
      "description": "An empty string cannot be parsed as a JSON schema; the method should reject it early."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"unknown\" } } }"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "JsonSchemaException (or a parsing exception) because the schema contains an unknown type",
      "description": "Invalid JSON schema content should cause a parsing/validation exception."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"$ref\": \"#/definitions/Person\", \"definitions\": { \"Person\": { \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } } } }"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "non‑null JsonSchema instance with the $ref correctly resolved",
      "description": "Schema containing an internal `$ref` should be resolved during loading; the returned schema must be usable."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"type\": \"object\" }"
        },
        {
          "parameter": "config",
          "value": "null"
        }
      ],
      "expected": "non‑null JsonSchema instance using the factory's default configuration",
      "description": "When `config` is `null` the method should fall back to the factory's default `SchemaValidatorsConfig` rather than throwing NPE."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "name: John\nage: 30"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "JsonSchemaException (or IOException) because the default input format is JSON but the content is YAML",
      "description": "The overload called defaults to `InputFormat.JSON`; providing YAML content should trigger a parsing error."
    }
  ]
}