Invalid control character at: line 14 column 53 (char 395)
{
  "testCases": [
    {
      "group": "Successful schema loading without preloading",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/simple-schema.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig() // defaults: preload = false"
            }
          ],
          "expected": "JsonSchema instance representing the parsed simple-schema.json",
          "description": "Verify that getSchema returns a JsonSchema object when a valid schema location is provided and the configuration does not request preloading. The method should call loadSchema, obtain the schema, invoke preload (which becomes a no‑op because preload is disabled), and finally return the schema."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"file:/tmp/absolute-schema.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig() // defaults"
            }
          ],
          "expected": "JsonSchema instance for the absolute file schema",
          "description": "Same as the previous case but using an absolute file URI to ensure that different URI schemes are handled correctly by loadSchema."
        }
      ]
    },
    {
      "group": "Successful schema loading with preloading enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/complex-schema.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setPreloadJsonSchema(true)"
            }
          ],
          "expected": "JsonSchema instance for complex-schema.json with all $ref schemas preloaded",
          "description": "When preload is enabled, getSchema must call preload which should resolve and cache any referenced schemas. The returned JsonSchema should be fully initialised."
        }
      ]
    },
    {
      "group": "Schema not found / invalid location",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/non‑existent.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchemaException (or RuntimeException) indicating that the schema could not be loaded",
          "description": "If loadSchema cannot locate the resource, getSchema should propagate the exception thrown by loadSchema."
        }
      ]
    },
    {
      "group": "Null parameters handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "NullPointerException or IllegalArgumentException because schemaUri is required",
          "description": "Validate that the method validates its arguments and fails fast when a null SchemaLocation is supplied."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/simple-schema.json\")"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "JsonSchema instance (default configuration is used internally)",
          "description": "If the configuration is null, the factory should fall back to a default SchemaValidatorsConfig and still return a valid JsonSchema."
        }
      ]
    },
    {
      "group": "Preload side‑effects verification",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/with‑ref.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setPreloadJsonSchema(true)"
            }
          ],
          "expected": "After getSchema returns, SchemaMappers.getCachedSchema(refUri) contains the referenced schema",
          "description": "When preloading is enabled, the preload method should have populated the internal cache with the referenced schemas. The test asserts the side‑effect rather than the return value."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/with‑ref.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setPreloadJsonSchema(false)"
            }
          ],
          "expected": "Cache remains empty for the referenced schema after getSchema returns",
          "description": "With preloading disabled, the referenced schema must not be cached; the preload call should be a no‑op."
        }
      ]
    }
  ]
}