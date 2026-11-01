{
  "testCases": [
    {
      "group": "Valid URI – Local resource schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "URI.create(\"classpath:/schemas/simple-schema.json\")"
            }
          ],
          "expected": "JsonSchema instance representing the parsed schema from simple-schema.json (no exception)",
          "description": "Pass a valid classpath URI pointing to a well‑formed JSON schema file. The method should delegate to the overload, load the schema and return a non‑null JsonSchema object."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "URI.create(\"file:/tmp/test-schema.json\")"
            }
          ],
          "expected": "JsonSchema instance representing the parsed schema from the file (no exception)",
          "description": "Pass a valid file‑system URI that points to a readable JSON schema file. The method should correctly convert the URI to a SchemaLocation and return the loaded JsonSchema."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "URI.create(\"http://example.com/schema.json\")"
            }
          ],
          "expected": "JsonSchema instance (or a SchemaLoadingException wrapped in a RuntimeException if network is unavailable)",
          "description": "Pass a remote HTTP URI. The factory should attempt to load the schema via the default SchemaLoader. The test verifies that the call path is exercised without throwing an unexpected exception."
        }
      ]
    },
    {
      "group": "Invalid URI – Non‑existent resource",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "URI.create(\"classpath:/schemas/nonexistent-schema.json\")"
            }
          ],
          "expected": "SchemaLoadingException (or a RuntimeException) indicating that the resource cannot be found",
          "description": "Provide a well‑formed URI that points to a resource that does not exist. The method should still delegate to the overload, which will fail to locate the schema and raise an appropriate exception."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "URI.create(\"file:/nonexistent/path/schema.json\")"
            }
          ],
          "expected": "SchemaLoadingException (or a RuntimeException) indicating file not found",
          "description": "Pass a file URI that points to a non‑existent file. The factory should attempt to read the file and propagate the loading failure."
        }
      ]
    },
    {
      "group": "Edge cases – Null and malformed URIs",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "Calling getSchema(null) should immediately throw a NullPointerException because the method dereferences schemaUri to call toString()."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "URI.create(\"ht!tp://bad_uri\")"
            }
          ],
          "expected": "URISyntaxException wrapped in IllegalArgumentException or RuntimeException",
          "description": "Supply a syntactically malformed URI. The factory will attempt to create a SchemaLocation from the string; the underlying implementation should raise an exception that the test can assert."
        }
      ]
    },
    {
      "group": "Configuration propagation – Custom SchemaValidatorsConfig",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "URI.create(\"classpath:/schemas/simple-schema.json\")"
            }
          ],
          "expected": "JsonSchema instance created with the default SchemaValidatorsConfig (verify that config properties such as 'typeLoose' are false)",
          "description": "Even though getSchema(URI) does not expose the config, it must use the factory's createSchemaValidatorsConfig(). The test inspects the returned JsonSchema's validation configuration to ensure the default config is applied."
        }
      ]
    }
  ]
}