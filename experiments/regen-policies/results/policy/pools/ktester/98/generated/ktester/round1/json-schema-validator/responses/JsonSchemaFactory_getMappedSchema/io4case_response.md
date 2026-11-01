{
  "testCases": [
    {
      "group": "Meta‑schema detection via $schema keyword",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "file:/tmp/draft07-schema.json"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchema instance created with JsonMetaSchema corresponding to Draft‑07 (the $schema keyword in the document is 'http://json-schema.org/draft-07/schema#'). The call to getMetaSchemaOrDefault must return the Draft‑07 meta‑schema instead of the factory default.",
          "description": "Loads a JSON schema file that contains a top‑level \"$schema\" keyword pointing to Draft‑07. The method should recognise the keyword, load the appropriate meta‑schema and create the JsonSchema using that meta‑schema."
        }
      ]
    },
    {
      "group": "Custom PathType in SchemaValidatorsConfig",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "file:/tmp/simple-schema.json"
            },
            {
              "parameter": "config",
              "value": "configWithJsonPointerPath"
            }
          ],
          "expected": "JsonSchema instance created successfully; the JsonNodePath used inside the method is instantiated with PathType.JSON_POINTER (as supplied by the custom config). No exception is thrown.",
          "description": "Verifies that the method respects the PathType supplied by a custom SchemaValidatorsConfig. The schema loads normally, but the internal evaluationPath must be built with the configured PathType."
        }
      ]
    },
    {
      "group": "SchemaLoader throws IOException before InputStream acquisition",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "file:/tmp/unreachable-schema.json"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchemaException with message 'Failed to load json schema from file:/tmp/unreachable-schema.json' and cause IOException thrown by SchemaLoader.getSchema(...).",
          "description": "The SchemaLoader is mocked to throw an IOException when getSchema(schemaUri) is called. The method must catch the IOException, log it and re‑throw a JsonSchemaException wrapping the original cause."
        }
      ]
    },
    {
      "group": "Fragment points to non‑existent sub‑schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "file:/tmp/complex-schema.json#/definitions/unknown"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchemaException (or a null return depending on implementation) indicating that the fragment '/definitions/unknown' cannot be resolved.",
          "description": "The schema loads successfully, but the fragment references a path that does not exist in the document. The method should attempt to retrieve the sub‑schema via getRefSchema and fail, resulting in an exception that propagates to the caller."
        }
      ]
    },
    {
      "group": "Remote HTTP URI without fragment",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/schemas/remote-schema.json"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchema instance created via doCreate with suppressSubSchemaRetrieval=true (no fragment handling).",
          "description": "Ensures that the method works with non‑file schemes (e.g., HTTP). The loader returns a valid InputStream, isYaml() returns false, and the method follows the 'schema without fragment' branch."
        }
      ]
    },
    {
      "group": "Unsupported file extension treated as JSON",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "file:/tmp/schema.txt"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchema instance created via doCreate with suppressSubSchemaRetrieval=true (no fragment handling).",
          "description": "A file with an unknown extension (.txt) should be processed as JSON because isYaml() returns false. The method must successfully parse the content and return a JsonSchema."
        }
      ]
    }
  ]
}