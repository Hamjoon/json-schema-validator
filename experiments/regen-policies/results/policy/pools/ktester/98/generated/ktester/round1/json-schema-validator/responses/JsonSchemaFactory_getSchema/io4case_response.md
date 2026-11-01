{
  "groups": [
    {
      "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - format handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"type: object\\nproperties:\\n  name:\\n    type: string\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "JsonProcessingException (or JsonSchemaException) because the default InputFormat.JSON cannot parse YAML content",
          "description": "A schema supplied in YAML format should cause a parsing exception when the factory assumes JSON input."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - configuration propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setTypeLoose(true)"
            }
          ],
          "expected": "non‑null JsonSchema whose ValidationContext reports typeLoose=true",
          "description": "When the config enables type‑loose mode, the created schema must carry that flag in its ValidationContext."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\", \"properties\": { \"date\": { \"type\": \"string\", \"format\": \"date-time\" } } }"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setValidateFormats(false)"
            }
          ],
          "expected": "non‑null JsonSchema whose ValidationContext has validateFormats=false (format validators are omitted)",
          "description": "Disabling format validation via the config should result in a schema that does not enforce the \"date-time\" format."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" } } }"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "non‑null JsonSchema equivalent to calling the method with a null config (default behavior)",
          "description": "Providing a freshly‑instantiated config with default values must behave the same as passing null."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - resource resolution edge cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"classpath:/schemas/person.json\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "non‑null JsonSchema loaded from the classpath resource (loader invoked)",
          "description": "A schema string that refers to a classpath resource should be resolved by the SchemaLoader and result in a valid JsonSchema."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"file:/tmp/example-schema.json\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "non‑null JsonSchema loaded from the file URI (loader invoked)",
          "description": "A schema string expressed as a file URI must be resolved via the loader and produce a schema instance."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - whitespace tolerance",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"   { \\\"type\\\": \\\"object\\\", \\\"properties\\\": { \\\"name\\\": { \\\"type\\\": \\\"string\\\" } } }   \\n\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "non‑null JsonSchema instance parsed successfully after trimming whitespace",
          "description": "Leading/trailing whitespace and newline characters around a valid JSON schema string should be ignored and not cause an error."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - $ref resolution failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\", \"properties\": { \"address\": { \"$ref\": \"#/nonexistent\" } } }"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "JsonSchemaException (or IllegalArgumentException) indicating unresolved $ref",
          "description": "A schema containing a $ref that cannot be resolved should cause schema loading to fail with an appropriate exception."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - caching behavior",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "\"http://example.com/schemas/cached.json\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "non‑null JsonSchema instance; subsequent call with the same URI returns the same object (identity equality)",
          "description": "When the same absolute URI is requested twice, the factory should return the cached JsonSchema instance rather than creating a new one."
        }
      ]
    }
  ]
}