{
  "testCases": [
    {
      "group": "Successful loading – YAML schema with fragment",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation pointing to a valid YAML schema file, absolute IRI = \"classpath:/schemas/complex.yaml#\\/definitions\\/address\", fragment not null and nameCount > 0"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig() with default PathType"
            }
          ],
          "expected": "A non‑null JsonSchema instance representing the sub‑schema referenced by the fragment is returned. The method must invoke getYamlMapper().readTree() (YAML branch) and then call doCreate with suppressSubSchemaRetrieval = false followed by document.getRefSchema(fragment).",
          "description": "Exercises the path where the schema file is YAML and a non‑empty fragment is present. Verifies that the YAML mapper is used, the full document schema is first created, and the fragment is resolved to the correct sub‑schema."
        }
      ]
    },
    {
      "group": "Failure – SchemaLoader throws IOException",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation pointing to a resource whose SchemaLoader.getSchema() throws an IOException (e.g., simulated network failure)."
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchemaException thrown with message containing \"Failed to load json schema from\" and its cause being the original IOException thrown by SchemaLoader.",
          "description": "Ensures that an IOException occurring *before* the InputStream is obtained (e.g., while locating the schema) is caught by the outer catch block, logged, wrapped in JsonSchemaException and re‑thrown."
        }
      ]
    },
    {
      "group": "Failure – Invalid meta‑schema identifier",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation pointing to a valid JSON schema file whose root node contains \"$schema\": \"http://example.com/unknown-meta-schema\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchemaException thrown (or any RuntimeException) originating from getMetaSchemaOrDefault when it cannot resolve the supplied meta‑schema identifier.",
          "description": "Tests the branch where getMetaSchemaOrDefault fails to locate a meta‑schema for the supplied \"$schema\" URI. The method does not catch this exception, so it should propagate unchanged."
        }
      ]
    },
    {
      "group": "Edge case – Null SchemaValidatorsConfig",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation pointing to a valid JSON schema file, absolute IRI = \"classpath:/schemas/simple.json\", fragment = null"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "NullPointerException thrown when the method attempts to call config.getPathType().",
          "description": "Validates that a null configuration argument is not tolerated and results in an NPE at the point where the method creates a JsonNodePath."
        }
      ]
    }
  ]
}