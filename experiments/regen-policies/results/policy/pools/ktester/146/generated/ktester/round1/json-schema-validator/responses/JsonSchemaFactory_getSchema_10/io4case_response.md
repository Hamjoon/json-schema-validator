{
  "tests": [
    {
      "group": "YAML schema handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/yaml-schema.yaml\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Returns a non‑null JsonSchema object; the YAML content is correctly parsed into a JsonNode and pre‑loaded without throwing an exception.",
          "description": "Verifies that a schema stored as YAML (detected by .yaml extension) is handled by getSchema, confirming that the internal YamlMapper is used."
        }
      ]
    },
    {
      "group": "Invalid URI handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"ht!tp://invalid@@uri\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws IllegalArgumentException (or URISyntaxException wrapped in RuntimeException) because the supplied schema location is not a valid URI.",
          "description": "Ensures that malformed URIs are detected early and result in a clear exception rather than an obscure loading error."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws IllegalArgumentException because an empty schema location string is not allowed.",
          "description": "Checks that an empty string passed to SchemaLocation is rejected."
        }
      ]
    },
    {
      "group": "Empty schema file",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/empty-schema.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws JsonSchemaException (or IOException) indicating that the schema content could not be parsed because the file is empty.",
          "description": "Validates that an empty schema resource is treated as a parsing error and propagated to the caller."
        }
      ]
    },
    {
      "group": "External $ref resolution",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/root-with-ref.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Returns a JsonSchema instance whose preload step resolves the external $ref located at classpath:/schemas/externally-referenced.json without throwing StackOverflowError.",
          "description": "Tests that preload correctly follows a $ref to another classpath resource and loads it, confirming proper interaction with SchemaLoaders."
        }
      ]
    },
    {
      "group": "Cache reuse behavior",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/simple-schema.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setCacheEnabled(true)"
            }
          ],
          "expected": "First call returns a non‑null JsonSchema; a second call with the same SchemaLocation and a config that also has cache enabled returns the *same* JsonSchema instance (reference equality).",
          "description": "Confirms that when caching is enabled, the factory reuses previously loaded schema objects, reducing loading overhead."
        }
      ]
    },
    {
      "group": "Configuration flag effects – typeLoose",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/type-loose-schema.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setTypeLoose(true)"
            }
          ],
          "expected": "Returns a JsonSchema object; the internal SchemaValidatorsConfig attached to the schema has typeLoose set to true, which will later affect validation of numeric/string type coercion.",
          "description": "Ensures that the typeLoose flag supplied via SchemaValidatorsConfig is preserved in the created JsonSchema, even though validation is not performed in this method."
        }
      ]
    },
    {
      "group": "Remote schema loading (mocked HTTP)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/schemas/remote-schema.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Returns a JsonSchema object when the underlying HTTP client (mocked) supplies a valid JSON schema; otherwise a JsonSchemaException is thrown. In the test environment the HTTP call is stubbed to return a known schema.",
          "description": "Verifies that getSchema can handle HTTP(S) URIs by delegating to the registered SchemaLoader; the test uses a mock HTTP responder to avoid network dependency."
        }
      ]
    },
    {
      "group": "Concurrent access – thread safety",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/simple-schema.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setCacheEnabled(true)"
            }
          ],
          "expected": "When invoked concurrently from multiple threads with the same arguments, all threads receive a non‑null JsonSchema instance and no race conditions or duplicate loads occur (the same cached instance is returned).",
          "description": "Ensures that the internal ConcurrentHashMap used for caching is correctly synchronized, preventing duplicate schema loading under concurrent usage."
        }
      ]
    }
  ]
}