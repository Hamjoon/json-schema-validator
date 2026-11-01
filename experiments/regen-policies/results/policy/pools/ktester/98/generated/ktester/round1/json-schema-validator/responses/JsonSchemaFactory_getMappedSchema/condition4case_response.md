{
  "testCases": [
    {
      "group": "Successful loads without fragment",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "file:/tmp/simple-schema.json"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchema instance created via doCreate with suppressSubSchemaRetrieval=true (no fragment handling).",
          "description": "Loads a JSON schema file (extension .json) where the URI has no fragment. The schemaLoader returns a non‑null InputStream, isYaml() returns false, readTree parses JSON, and the method follows the \"Schema without fragment\" branch."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "file:/tmp/simple-schema.yaml"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchema instance created via doCreate with suppressSubSchemaRetrieval=true (no fragment handling).",
          "description": "Loads a YAML schema file (extension .yaml) with no fragment. The isYaml() check returns true, readTree parses YAML, and the method follows the same branch as above."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "file:/tmp/simple-schema.json#"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchema instance created via doCreate with suppressSubSchemaRetrieval=true (no fragment handling).",
          "description": "URI contains an empty fragment ('#'). The fragment object exists but its nameCount is 0, therefore the method treats it as a schema without fragment and follows the first branch."
        }
      ]
    },
    {
      "group": "Successful loads with fragment (sub‑schema retrieval)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "file:/tmp/complex-schema.json#/definitions/address"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchema instance representing the sub‑schema located at /definitions/address.",
          "description": "Loads a JSON schema that contains a fragment pointing to a sub‑schema. The method follows the \"Schema with fragment\" branch: creates a document schema, then calls getRefSchema on the fragment and returns the referenced sub‑schema."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "file:/tmp/complex-schema.yaml#/components/schemas/Person"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchema instance representing the sub‑schema located at /components/schemas/Person.",
          "description": "Same as above but the source file is YAML, so isYaml() returns true and readTree parses YAML before the fragment handling logic."
        }
      ]
    },
    {
      "group": "Error handling – missing InputStream",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "file:/tmp/missing-schema.json"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchemaException with message \"Failed to load json schema from file:/tmp/missing-schema.json\" and cause IOException(\"Cannot load schema at file:/tmp/missing-schema.json\").",
          "description": "The schemaLoader returns a SchemaResource whose getInputStream() returns null. The method throws an IOException which is caught and wrapped in a JsonSchemaException."
        }
      ]
    },
    {
      "group": "Error handling – readTree throws IOException",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "file:/tmp/bad-schema.json"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchemaException with message \"Failed to load json schema from file:/tmp/bad-schema.json\" and cause the original IOException from readTree.",
          "description": "The InputStream is non‑null, but readTree throws an IOException (e.g., malformed JSON). The catch block converts it to a JsonSchemaException."
        }
      ]
    }
  ]
}