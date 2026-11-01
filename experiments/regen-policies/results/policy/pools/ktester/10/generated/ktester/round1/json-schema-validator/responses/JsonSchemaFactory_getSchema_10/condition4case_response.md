{
  "testCases": [
    {
      "group": "Cache Disabled (enableUriSchemaCache = false)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new TestSchemaLocation(\"http://example.com/schema1\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "The method returns the JsonSchema produced by getMappedSchema(). getMappedSchema() must be invoked exactly once and withConfig() must NOT be called because the returned schema is already configured.",
          "description": "When URI schema caching is disabled the factory should bypass the cache and directly delegate to getMappedSchema(). The returned schema is the raw schema (no additional withConfig() call)."
        }
      ]
    },
    {
      "group": "Cache Enabled – Cache Miss (first call)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new TestSchemaLocation(\"http://example.com/schema2\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "The method invokes getMappedSchema() once, stores the result in uriSchemaCache, calls withConfig(config) on the cached schema and returns the configured schema.",
          "description": "With caching enabled, the first request for a given SchemaLocation should miss the cache, trigger getMappedSchema() via computeIfAbsent, cache the raw schema, then apply the supplied config before returning."
        }
      ]
    },
    {
      "group": "Cache Enabled – Cache Hit (subsequent call)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new TestSchemaLocation(\"http://example.com/schema2\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "The method does NOT invoke getMappedSchema() (cache hit). It retrieves the previously cached JsonSchema, calls withConfig(config) on it, and returns the configured schema.",
          "description": "A second call with the same SchemaLocation while caching is enabled should hit the cache; the lambda supplied to computeIfAbsent must not be executed, confirming that the cached entry is reused."
        }
      ]
    },
    {
      "group": "Cache Enabled – Different SchemaLocation (second distinct key)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new TestSchemaLocation(\"http://example.com/schema3\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "The method invokes getMappedSchema() for the new key, stores it in the cache, calls withConfig(config) and returns the configured schema.",
          "description": "When a different SchemaLocation is requested, even with caching enabled, the cache should miss for that key and behave like the first‑miss scenario."
        }
      ]
    }
  ]
}