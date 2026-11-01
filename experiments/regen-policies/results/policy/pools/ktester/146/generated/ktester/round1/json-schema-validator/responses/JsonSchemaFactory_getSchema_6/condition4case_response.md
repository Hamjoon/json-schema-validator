{
  "testCases": [
    {
      "group": "Valid URI – schema loaded successfully",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"classpath:/schemas/simple-schema.json\")"
            }
          ],
          "expected": "JsonSchema instance is returned and its root node matches the JSON content of simple-schema.json",
          "description": "A well‑formed classpath URI that points to an existing JSON schema file. The method should resolve the URI, read the schema and return a non‑null JsonSchema object."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"file:/tmp/external-schema.json\")"
            }
          ],
          "expected": "JsonSchema instance is returned and its root node matches the JSON content of external-schema.json",
          "description": "A file‑system URI that points to a readable schema file. The method should use the default SchemaLoader to load the file and return a valid JsonSchema."
        }
      ]
    },
    {
      "group": "Invalid URI – malformed or unsupported scheme",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"ht!tp://bad[uri]\")"
            }
          ],
          "expected": "java.net.URISyntaxException thrown before getSchema(URI) is invoked",
          "description": "The URI string is syntactically invalid; construction of the URI itself fails, so getSchema is never called."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"mailto:user@example.com\")"
            }
          ],
          "expected": "IllegalArgumentException with message containing \"Unsupported schema URI scheme\"",
          "description": "The URI uses a scheme that the factory does not know how to resolve (e.g., mailto). The method should detect this and throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Null argument handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            }
          ],
          "expected": "NullPointerException thrown",
          "description": "Passing a null URI should cause a NullPointerException because the implementation immediately calls schemaUri.toString()."
        }
      ]
    },
    {
      "group": "Non‑existent resource – loading failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"classpath:/schemas/missing-schema.json\")"
            }
          ],
          "expected": "JsonSchemaException (or IOException) indicating that the resource could not be found",
          "description": "The URI is syntactically correct but points to a resource that does not exist on the classpath. The method should attempt to load it and propagate a meaningful exception."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"file:/nonexistent/path/schema.json\")"
            }
          ],
          "expected": "JsonSchemaException (or IOException) indicating file not found",
          "description": "A file‑system URI that refers to a file that cannot be read. The method should surface the I/O problem."
        }
      ]
    },
    {
      "group": "Custom SchemaValidatorsConfig effect",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"classpath:/schemas/with‑defaults.json\")"
            }
          ],
          "expected": "JsonSchema instance whose configuration reflects the defaults created by createSchemaValidatorsConfig() (e.g., $ref handling enabled)",
          "description": "Ensures that getSchema(URI) forwards the default SchemaValidatorsConfig produced by createSchemaValidatorsConfig() to the underlying loader."
        }
      ]
    }
  ]
}