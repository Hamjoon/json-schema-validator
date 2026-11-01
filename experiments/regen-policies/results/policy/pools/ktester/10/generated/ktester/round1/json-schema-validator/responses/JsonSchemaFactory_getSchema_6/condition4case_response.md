{
  "testCases": [
    {
      "group": "Valid URI - Local JSON Schema Resource",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"classpath:/schemas/simple-schema.json\")"
            }
          ],
          "expected": "JsonSchema instance is returned and its meta-schema version matches the schema's $schema declaration (e.g., DraftV7).",
          "description": "Verify that getSchema(URI) correctly loads a valid JSON schema located on the classpath and returns a non‑null JsonSchema object."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"file:/tmp/test-schema.json\")"
            }
          ],
          "expected": "JsonSchema instance is returned and its root node equals the JSON content of the file.",
          "description": "Confirm that a file‑system URI pointing to a valid JSON schema file is resolved and a proper JsonSchema object is produced."
        }
      ]
    },
    {
      "group": "Valid URI - Remote HTTP JSON Schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"https://example.com/schemas/remote-schema.json\")"
            }
          ],
          "expected": "JsonSchema instance is returned; the factory performs an HTTP GET and parses the response without throwing.",
          "description": "Ensure that an HTTP/HTTPS URI is accepted, the schema is fetched via the default ResourceFetcher, and a JsonSchema is created."
        }
      ]
    },
    {
      "group": "Invalid URI - Null Argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "Calling getSchema(null) should throw a NullPointerException because the method dereferences schemaUri.toString()."
        }
      ]
    },
    {
      "group": "Invalid URI - Malformed Syntax",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"ht!tp://[invalid]\")"
            }
          ],
          "expected": "IllegalArgumentException or JsonSchemaException indicating the URI cannot be resolved",
          "description": "Pass a syntactically malformed URI; the factory should fail to locate a schema and raise an appropriate exception."
        }
      ]
    },
    {
      "group": "Non‑existent Resource",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"classpath:/schemas/missing-schema.json\")"
            }
          ],
          "expected": "JsonSchemaException (or IOException) indicating the resource could not be found",
          "description": "When the URI points to a location that does not contain a schema, the loader must report the missing resource."
        }
      ]
    },
    {
      "group": "URI with Fragment Identifier",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"classpath:/schemas/complex-schema.json#definitions/address\")"
            }
          ],
          "expected": "JsonSchema instance whose root node corresponds to the fragment definition (the address schema).",
          "description": "Validate that a URI containing a fragment resolves to the correct sub‑schema within the document."
        }
      ]
    },
    {
      "group": "URI with Query Parameters (ignored by loader)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"classpath:/schemas/simple-schema.json?version=1\")"
            }
          ],
          "expected": "JsonSchema instance is returned; query part is ignored and does not affect loading.",
          "description": "Confirm that additional query parameters do not break schema loading; the loader should strip them before locating the resource."
        }
      ]
    }
  ]
}