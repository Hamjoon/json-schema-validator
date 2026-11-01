{
  "testCases": [
    {
      "group": "Invalid schemaUri format – malformed URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"ht!tp://[invalid]\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree('{\"type\":\"string\"}')"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException thrown from getSchemaLocation because the URI cannot be parsed.",
          "description": "Ensures that a schema location containing a syntactically invalid URI is rejected early, preventing further processing."
        }
      ]
    },
    {
      "group": "Empty schemaUri string – should be treated as missing",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree('{\"type\":\"integer\"}')"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException (or NullPointerException) thrown because an empty location is not a valid schema identifier.",
          "description": "Verifies that the factory does not accept an empty string as a schema location and fails fast."
        }
      ]
    },
    {
      "group": "Schema validation enabled – invalid schema should raise exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/invalid-schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree('{\"type\":\"unknown-type\"}')"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setValidateSchema(true)"
            }
          ],
          "expected": "JsonSchemaException (or ValidationException) thrown during preload because the schema is not compliant with the meta‑schema.",
          "description": "When validation of the supplied schema is turned on, the factory must check the schema against its meta‑schema and reject an illegal \"type\" value."
        }
      ]
    },
    {
      "group": "Custom path type – JSON_PATH (if supported)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/my-schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree('{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}}}')"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setPathType(JsonNodePath.Type.JSON_PATH)"
            }
          ],
          "expected": "A non‑null JsonSchema instance is returned; the ValidationContext inside the schema uses JSON_PATH as its path representation.",
          "description": "Checks that a non‑default path type (JSON_PATH) is correctly propagated to the ValidationContext and reflected in error messages/paths."
        }
      ]
    },
    {
      "group": "Combined flags – failFast true and suppressSubSchemaRetrieval true",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/combined.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree('{\"$ref\":\"http://example.com/other.json\"}')"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setFailFast(true).setSuppressSubSchemaRetrieval(true)"
            }
          ],
          "expected": "A non‑null JsonSchema instance is returned; preload does not attempt to resolve the $ref because suppression is active, and any validation failures would be reported immediately due to fail‑fast mode.",
          "description": "Validates that the factory honours multiple configuration flags simultaneously without conflict."
        }
      ]
    },
    {
      "group": "Explicit default config – config object with all defaults set explicitly",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/explicit-defaults.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree('{\"type\":\"boolean\"}')"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "A non‑null JsonSchema instance is returned; behavior is identical to passing null config.",
          "description": "Ensures that providing a freshly instantiated config that has not been modified behaves the same as using the default (null) configuration."
        }
      ]
    },
    {
      "group": "SchemaNode is an empty object – should still produce a JsonSchema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/empty-object.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree('{}')"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "A non‑null JsonSchema instance is returned; the schema represents an unrestricted schema (any JSON value is valid).",
          "description": "Verifies that an empty JSON object, which is a valid (though unrestricted) schema, is accepted by the factory."
        }
      ]
    },
    {
      "group": "SchemaNode is a JSON array – not a valid schema root",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/array-root.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree('[{\"type\":\"string\"}]')"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "JsonSchemaException (or IllegalArgumentException) thrown because the root of a schema must be an object, not an array.",
          "description": "Ensures that the factory validates the structural requirement that a JSON Schema document must be a JSON object."
        }
      ]
    }
  ]
}