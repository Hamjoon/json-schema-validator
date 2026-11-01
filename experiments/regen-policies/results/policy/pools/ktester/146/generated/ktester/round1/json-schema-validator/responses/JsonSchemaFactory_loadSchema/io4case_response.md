{
  "newTestCases": [
    {
      "group": "Cache enabled – cache hit with different config",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/schema4.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setFailFast(true)"
            }
          ],
          "expected": "First call returns schema from getMappedSchema.withConfig(configA) and stores it; second call with a different config returns cachedSchema.withConfig(configB) (getMappedSchema not invoked again).",
          "description": "Populate the cache with a schema using config A, then call loadSchema again with the same URI but a different config B. Verify that the method does not call getMappedSchema a second time and that the returned schema is the cached instance after applying withConfig(configB)."
        }
      ]
    },
    {
      "group": "Cache enabled – null config argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/schema5.json\")"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "JsonSchema instance returned from cachedSchema.withConfig(null) (no NullPointerException).",
          "description": "When caching is enabled and the supplied SchemaValidatorsConfig is null, loadSchema should still return a JsonSchema (the cached one) after invoking withConfig(null). This checks that the method tolerates a null config argument."
        }
      ]
    },
    {
      "group": "Cache disabled – null schemaUri",
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
          "expected": "NullPointerException thrown (schemaUri is required for getMappedSchema).",
          "description": "When caching is disabled and a null SchemaLocation is passed, the method should propagate the NPE that occurs inside getMappedSchema (or earlier). This validates argument validation for the URI parameter."
        }
      ]
    },
    {
      "group": "Cache enabled – pre‑populated cache entry",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/prepopulated.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setReadOnly(true)"
            }
          ],
          "expected": "JsonSchema instance returned from pre‑populated cachedSchema.withConfig(config) without invoking getMappedSchema.",
          "description": "Manually insert a JsonSchema into the factory's internal cache before invoking loadSchema. Verify that loadSchema returns the cached schema after applying withConfig and never calls getMappedSchema."
        }
      ]
    },
    {
      "group": "Cache enabled – getMappedSchema returns null after a previous successful mapping",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/flipflop.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "First call returns a JsonSchema (stored in cache). Second call returns the same JsonSchema (cached) even though getMappedSchema would now return null.",
          "description": "Simulate a scenario where the first invocation maps the URI successfully and caches it, then alter the underlying loader so that subsequent getMappedSchema calls would return null. The second loadSchema call must still return the cached schema, proving that the cache shields later mapping failures."
        }
      ]
    }
  ]
}