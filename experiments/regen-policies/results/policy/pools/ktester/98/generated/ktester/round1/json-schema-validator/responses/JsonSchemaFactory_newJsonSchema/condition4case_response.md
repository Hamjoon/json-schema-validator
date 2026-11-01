{
  "testCases": [
    {
      "group": "Normal operation – valid inputs, default config",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "ObjectMapper().readTree('{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}')"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "A non‑null JsonSchema instance is returned; no exception is thrown.",
          "description": "Verifies the happy‑path where a proper schema location, a valid JSON schema node and the default (null) configuration are supplied. The method should create a ValidationContext, delegate to doCreate, preload the schema and finally return a JsonSchema object."
        }
      ]
    },
    {
      "group": "Normal operation – custom SchemaValidatorsConfig",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/my-schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "ObjectMapper().readTree('{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}')"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setPathType(JsonNodePath.Type.JSON_POINTER).setFailFast(true)"
            }
          ],
          "expected": "A non‑null JsonSchema instance is returned; the ValidationContext inside the created schema must reflect the custom path type and fail‑fast flag.",
          "description": "Ensures that a non‑null configuration is correctly passed to createValidationContext and that the resulting JsonSchema respects the supplied configuration (e.g., path type JSON‑Pointer and fail‑fast mode)."
        }
      ]
    },
    {
      "group": "Null schemaUri",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            },
            {
              "parameter": "schemaNode",
              "value": "ObjectMapper().readTree('{\"type\":\"string\"}')"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException (or NullPointerException depending on implementation) thrown from getSchemaLocation or downstream processing.",
          "description": "Tests the behaviour when the schema location argument is null. The factory should reject a null location early (via getSchemaLocation) and raise an appropriate exception."
        }
      ]
    },
    {
      "group": "Null schemaNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/empty.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "NullPointerException thrown from createValidationContext (or a custom IllegalArgumentException).",
          "description": "A null JsonNode does not represent a valid schema. The factory should detect this and fail before attempting to create a JsonSchema."
        }
      ]
    },
    {
      "group": "Empty SchemaValidatorsConfig (all defaults)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/empty-schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "ObjectMapper().readTree('{\"type\":\"null\"}')"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()" 
            }
          ],
          "expected": "A non‑null JsonSchema instance is returned; preload() should be a no‑op because the config contains no special flags.",
          "description": "Confirms that an explicitly created but otherwise default configuration does not alter the normal flow and that preload() does not attempt any extra loading."
        }
      ]
    },
    {
      "group": "Config that disables $ref loading (suppressSubSchemaRetrieval scenario)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/ref-schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "ObjectMapper().readTree('{\"$ref\":\"http://example.com/other.json\"}')"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setSuppressSubSchemaRetrieval(true)"
            }
          ],
          "expected": "A non‑null JsonSchema instance is returned; the internal doCreate call receives suppressSubSchemaRetrieval = false (as per method signature) but preload() respects the config and does not attempt to resolve the $ref.",
          "description": "Ensures that when the configuration asks to suppress sub‑schema retrieval, the factory does not try to load referenced schemas during preload, avoiding unnecessary I/O or recursion."
        }
      ]
    }
  ]
}