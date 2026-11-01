{
  "testCases": [
    {
      "group": "Valid inputs – happy path",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "mockSchemaLocation"
            },
            {
              "parameter": "schemaNode",
              "value": "mockJsonNode"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "returns a non‑null JsonSchema instance; doCreate is invoked once with the ValidationContext created from the supplied schemaNode and config, the normalized SchemaLocation, a JsonNodePath built from the config's path type, the supplied schemaNode, parentSchema = null and suppressSubSchemaRetrieval = false; preload is invoked with the created JsonSchema and the supplied config.",
          "description": "Ensures that with normal, non‑null arguments the method creates a ValidationContext, delegates to doCreate, pre‑loads the schema and returns the created JsonSchema."
        }
      ]
    },
    {
      "group": "Null SchemaValidatorsConfig – default configuration fallback",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "mockSchemaLocation"
            },
            {
              "parameter": "schemaNode",
              "value": "mockJsonNode"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "returns a non‑null JsonSchema; createValidationContext is called with a null config and must internally create a default SchemaValidatorsConfig; the subsequent doCreate and preload calls behave as in the happy path.",
          "description": "Verifies that when the config argument is null the factory falls back to its default configuration rather than throwing an exception."
        }
      ]
    },
    {
      "group": "Null JsonNode – should reject missing schema definition",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "mockSchemaLocation"
            },
            {
              "parameter": "schemaNode",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "throws NullPointerException (or IllegalArgumentException) because schemaNode is required for ValidationContext creation.",
          "description": "Ensures that the method validates its mandatory schemaNode argument and fails fast when it is null."
        }
      ]
    },
    {
      "group": "Null SchemaLocation – factory must handle missing location",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            },
            {
              "parameter": "schemaNode",
              "value": "mockJsonNode"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "returns a non‑null JsonSchema; getSchemaLocation receives null and should return null (or a default placeholder) which is then passed to doCreate; the method must not throw an exception solely because schemaUri is null.",
          "description": "Tests that the method tolerates a null schema location, delegating the null value to doCreate, which is expected to handle it gracefully."
        }
      ]
    },
    {
      "group": "Config with custom PathType – verifies JsonNodePath construction",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "mockSchemaLocation"
            },
            {
              "parameter": "schemaNode",
              "value": "mockJsonNode"
            },
            {
              "parameter": "config",
              "value": "customPathConfig (pathType = JSON_POINTER)"
            }
          ],
          "expected": "returns a JsonSchema; the JsonNodePath passed to doCreate is built using the custom path type (JSON_POINTER) from the supplied config.",
          "description": "Confirms that the method respects the path type defined in the supplied SchemaValidatorsConfig when constructing the JsonNodePath."
        }
      ]
    }
  ]
}