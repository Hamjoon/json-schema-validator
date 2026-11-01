{
  "testCases": [
    {
      "group": "Cache disabled – direct mapping",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/schema1.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance returned from getMappedSchema (non‑null)",
          "description": "When enableSchemaCache is false the method should bypass the cache and return the schema produced by getMappedSchema directly."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/unknown.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "null",
          "description": "When enableSchemaCache is false and getMappedSchema returns null, loadSchema must propagate the null result."
        }
      ]
    },
    {
      "group": "Cache enabled – cache miss, mapping succeeds",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/schema2.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance returned from getMappedSchema.withConfig(config) and stored in cache",
          "description": "First call with a fresh URI while caching is enabled should invoke getMappedSchema, store the non‑null schema in the cache and return the result of schema.withConfig(config)."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/schema2.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance returned from cachedSchema.withConfig(config) (getMappedSchema not called again)",
          "description": "A second call with the same URI should hit the cache, skip getMappedSchema, and still invoke withConfig on the cached schema."
        }
      ]
    },
    {
      "group": "Cache enabled – cache miss, mapping returns null",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/invalid.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "null and cache remains empty for this URI",
          "description": "When enableSchemaCache is true but getMappedSchema returns null, loadSchema must return null and must not place a null entry into the cache."
        }
      ]
    },
    {
      "group": "Cache enabled – concurrent‑like re‑check path",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/schema3.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance returned from getMappedSchema.withConfig(config)",
          "description": "Simulate a race where the cache is checked twice (outside and inside the synchronized block). The test verifies that the second check does not affect the result and that the schema is stored exactly once."
        }
      ]
    }
  ]
}