{
  "testCases": [
    {
      "group": "Successful loading – JSON schema without fragment",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation pointing to a valid JSON schema file, absolute IRI = \"classpath:/schemas/simple.json\", fragment = null"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig() with default PathType"
            }
          ],
          "expected": "A non‑null JsonSchema instance returned. The returned schema is created via doCreate with suppressSubSchemaRetrieval = true. No calls to getYamlMapper().readTree().",
          "description": "Verifies the happy path when the schema file is JSON, there is no fragment in the URI, and the InputStream is successfully opened. The method should read the JSON, resolve the meta‑schema, build a ValidationContext and call doCreate with suppressSubSchemaRetrieval = true."
        }
      ]
    },
    {
      "group": "Successful loading – YAML schema without fragment",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation pointing to a valid YAML schema file, absolute IRI = \"classpath:/schemas/simple.yaml\", fragment = null"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "A non‑null JsonSchema instance returned. The method should invoke getYamlMapper().readTree() instead of getJsonMapper().readTree().",
          "description": "Ensures the isYaml() branch is exercised: when the URI ends with .yaml/.yml the loader must use the YAML mapper."
        }
      ]
    },
    {
      "group": "Successful loading – JSON schema with fragment",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation with absolute IRI = \"classpath:/schemas/complex.json#\\/definitions\\/address\", fragment not null and nameCount > 0"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "A JsonSchema representing the sub‑schema referenced by the fragment is returned. The method should create a ValidationContext via createValidationContext(), call doCreate with suppressSubSchemaRetrieval = false, then call document.getRefSchema(fragment).",
          "description": "Tests the branch where a fragment is present. The method must load the whole document schema first, then resolve the fragment to a sub‑schema."
        }
      ]
    },
    {
      "group": "Failure – InputStream is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation pointing to a non‑existent resource, absolute IRI = \"classpath:/schemas/missing.json\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchemaException thrown with message containing \"Failed to load json schema from\" and cause = IOException with message \"Cannot load schema at classpath:/schemas/missing.json\".",
          "description": "Simulates the situation where SchemaLoader returns a SchemaReference whose getInputStream() returns null. The method should throw an IOException which is wrapped into JsonSchemaException."
        }
      ]
    },
    {
      "group": "Failure – IOException while reading schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation pointing to a valid resource but whose InputStream throws IOException on read (e.g., corrupted zip entry)."
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchemaException thrown; its cause is the original IOException. The logger should record an error with the absolute IRI.",
          "description": "Covers the catch block. Any IOException occurring inside the try‑with‑resources (including from readTree()) must be caught, logged and re‑thrown as JsonSchemaException."
        }
      ]
    },
    {
      "group": "Edge case – Fragment present but empty (nameCount == 0)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation with absolute IRI = \"classpath:/schemas/simple.json#\" (fragment exists but has zero path segments)."
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Same behaviour as the \"no fragment\" branch: doCreate called with suppressSubSchemaRetrieval = true and the full schema returned.",
          "description": "Ensures that a fragment object with nameCount == 0 is treated as if there were no fragment, exercising the conditional `(schemaUri.getFragment() == null) || (schemaUri.getFragment().getNameCount() == 0)`."
        }
      ]
    }
  ]
}