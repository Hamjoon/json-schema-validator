{
  "testCases": [
    {
      "group": "validSchemaLoading",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "classpath:/schemas/simple-schema.json"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "JsonSchema instance representing the parsed schema (non‑null, no exception)",
          "description": "Load a valid JSON Schema from the classpath using a null configuration, expecting a successfully built JsonSchema object."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "file:/tmp/test-schema.json"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance (non‑null) with default validation settings",
          "description": "Load a schema from a file system path with an explicitly created default configuration; the factory should return a valid JsonSchema."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "classpath:/schemas/with‑ref-schema.json"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance (non‑null) where all $ref references are resolved during preload",
          "description": "Schema contains internal $ref references; after getSchema the preload step must resolve them, so the returned JsonSchema is ready for validation without further loading."
        }
      ]
    },
    {
      "group": "nullParameters",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Both parameters are null – the factory should reject the call and throw an IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "classpath:/schemas/simple-schema.json"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "JsonSchema instance (non‑null)",
          "description": "A null config should be treated as the default configuration; the method must still return a valid JsonSchema."
        }
      ]
    },
    {
      "group": "invalidSchemaUri",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "classpath:/schemas/non‑existent.json"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchemaException (or IOException) indicating that the schema resource cannot be found",
          "description": "The supplied SchemaLocation points to a resource that does not exist; the factory should fail with a clear exception."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://invalid-host/schema.json"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchemaException (or IOException) caused by network/IO failure",
          "description": "A remote URI that cannot be resolved should cause the factory to throw an exception rather than returning a null schema."
        }
      ]
    },
    {
      "group": "circularReference",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "classpath:/schemas/circular-a.json"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance (non‑null) with circular $ref resolved without stack overflow",
          "description": "Two schemas reference each other (circular $ref). The preload step must detect and handle the cycle, returning a usable JsonSchema."
        }
      ]
    },
    {
      "group": "cachingBehavior",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "classpath:/schemas/simple-schema.json"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Two successive calls return the same JsonSchema instance (==)",
          "description": "JsonSchemaFactory caches loaded schemas; calling getSchema twice with the same location and config should return the identical object."
        }
      ]
    },
    {
      "group": "customConfigEffect",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "classpath:/schemas/type‑loose-schema.json"
            },
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig { typeLoose = true }"
            }
          ],
          "expected": "JsonSchema instance (non‑null) where type‑loose validation is enabled",
          "description": "When the config enables typeLoose, the created JsonSchema must reflect this setting; subsequent validation would allow type coercion."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "classpath:/schemas/type‑loose-schema.json"
            },
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig { typeLoose = false }"
            }
          ],
          "expected": "JsonSchema instance (non‑null) where type‑loose validation is disabled",
          "description": "Same schema but with typeLoose disabled; the factory must honour the config and produce a schema that enforces strict typing."
        }
      ]
    }
  ]
}