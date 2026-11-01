{
  "testCases": [
    {
      "group": "Cache disabled - direct mapping",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "mockSchemaLocation1"
            },
            {
              "parameter": "config",
              "value": "mockConfigA"
            }
          ],
          "expected": "returns the JsonSchema instance produced by getMappedSchema (no caching performed)",
          "description": "When enableSchemaCache is false the method must bypass the cache completely and delegate to getMappedSchema. The returned object should be exactly what getMappedSchema returns."
        }
      ]
    },
    {
      "group": "Cache enabled - cache miss, mapper returns non‑null schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "mockSchemaLocation2"
            },
            {
              "parameter": "config",
              "value": "mockConfigB"
            }
          ],
          "expected": "returns the result of cachedSchema.withConfig(mockConfigB) and stores cachedSchema in the internal cache",
          "description": "With enableSchemaCache true and an empty cache, loadSchema should call getMappedSchema, put the returned schema into the cache, and finally return the schema after applying withConfig."
        }
      ]
    },
    {
      "group": "Cache enabled - cache miss, mapper returns null",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "mockSchemaLocation3"
            },
            {
              "parameter": "config",
              "value": "mockConfigC"
            }
          ],
          "expected": "returns null and does not modify the cache",
          "description": "When enableSchemaCache is true but getMappedSchema returns null, loadSchema must propagate the null result without inserting anything into the cache."
        }
      ]
    },
    {
      "group": "Cache enabled - cache hit",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "mockSchemaLocation4"
            },
            {
              "parameter": "config",
              "value": "mockConfigD"
            }
          ],
          "expected": "returns the cached JsonSchema instance after invoking withConfig(mockConfigD); getMappedSchema is never called",
          "description": "If a schema is already present in the cache, loadSchema must return the cached value (after applying withConfig) and must not call getMappedSchema again."
        }
      ]
    }
  ]
}