{
  "testCases": [
    {
      "group": "Null SchemaLocation – Cache Disabled",
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
          "expected": "NullPointerException is thrown because schemaUri is null and the method attempts to use it as a key for the cache/computed lookup.",
          "description": "When caching is disabled the method does not guard against a null SchemaLocation; passing null should result in an immediate NPE."
        }
      ]
    },
    {
      "group": "Null SchemaLocation – Cache Enabled",
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
          "expected": "NullPointerException is thrown because the null key cannot be used with ConcurrentMap.computeIfAbsent.",
          "description": "With caching enabled the lambda passed to computeIfAbsent receives a null key, which should cause an NPE."
        }
      ]
    },
    {
      "group": "Null Config Parameter",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new TestSchemaLocation(\"http://example.com/schema-null-config\")"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "NullPointerException is thrown when withConfig(null) is invoked on the cached/raw JsonSchema.",
          "description": "The factory always calls withConfig on the schema; passing a null config should cause an NPE (the real implementation validates the argument)."
        }
      ]
    },
    {
      "group": "Exception Propagation from getMappedSchema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new TestSchemaLocation(\"http://example.com/invalid-schema\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IOException (or RuntimeException) thrown by getMappedSchema is propagated to the caller unchanged.",
          "description": "If the underlying schema loading fails (e.g., malformed JSON/YAML), getSchema must not swallow the exception; it should bubble up."
        }
      ]
    },
    {
      "group": "Cache Enabled – Concurrent First‑Miss",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new TestSchemaLocation(\"http://example.com/concurrent-schema\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "getMappedSchema is invoked exactly once despite two concurrent getSchema calls; both threads receive JsonSchema instances that are the result of withConfig(config).",
          "description": "Simulate two threads calling getSchema simultaneously with the same SchemaLocation while caching is enabled. The computeIfAbsent contract guarantees the mapping function runs once."
        }
      ]
    },
    {
      "group": "Cache Enabled – withConfig Returns New Instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new TestSchemaLocation(\"http://example.com/different-config\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setFailFast(true)"
            }
          ],
          "expected": "The JsonSchema returned by getSchema is NOT the same object that was stored in the cache; it is the result of calling withConfig(config) on the cached raw schema.",
          "description": "Verify that the factory does not return the raw cached schema but always the configured wrapper."
        }
      ]
    },
    {
      "group": "Cache Enabled – Changing Config Between Calls",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new TestSchemaLocation(\"http://example.com/multi-config\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setFailFast(false)"
            }
          ],
          "expected": "First call caches the raw schema; second call (same URI, different config) does NOT invoke getMappedSchema again, but calls withConfig on the cached raw schema with the new config, returning a different configured instance.",
          "description": "Ensures that the cache stores the un‑configured schema and that each call can apply a distinct configuration."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new TestSchemaLocation(\"http://example.com/multi-config\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setFailFast(true)"
            }
          ],
          "expected": "Same as previous case – getMappedSchema is NOT called again; withConfig is invoked with the new config, producing a different JsonSchema object.",
          "description": "Second invocation with a different config validates that withConfig is applied per call."
        }
      ]
    },
    {
      "group": "Cache Disabled – Multiple Sequential Calls",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new TestSchemaLocation(\"http://example.com/no-cache-multi\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "getMappedSchema is invoked on each call (twice for two sequential invocations); no caching occurs, and the returned schemas are the raw results from getMappedSchema (no withConfig call).",
          "description": "Confirms that disabling the cache forces a fresh mapping for every request."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new TestSchemaLocation(\"http://example.com/no-cache-multi\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Same as previous – getMappedSchema called again; the two returned JsonSchema instances are distinct.",
          "description": "Second sequential call should still bypass any cache."
        }
      ]
    },
    {
      "group": "Cache Key Sensitivity – Fragment Identifier",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new TestSchemaLocation(\"http://example.com/schema#fragmentA\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "When caching is enabled, a schema with fragment '#fragmentA' is cached under its full URI; a later request with '#fragmentB' is treated as a distinct key and triggers another getMappedSchema call.",
          "description": "Ensures that the cache distinguishes between URIs that differ only by fragment."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new TestSchemaLocation(\"http://example.com/schema#fragmentB\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "getMappedSchema is invoked again because the fragment makes the key different; the returned schema is cached separately.",
          "description": "Second call with a different fragment validates separate cache entries."
        }
      ]
    }
  ]
}