{
  "testCases": [
    {
      "group": "JsonSchemaFactory.getSchema(SchemaLocation) - malformed URI and I/O errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://[invalid-uri]\")"
            }
          ],
          "expected": "IllegalArgumentException (malformed URI syntax)",
          "description": "The supplied URI string cannot be parsed according to RFC 2396; the factory should reject it with an IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/not‑found.json\")"
            }
          ],
          "expected": "JsonSchemaException (or IOException) indicating HTTP 404 Not Found",
          "description": "The HTTP endpoint exists but returns a 404 status; the loader must surface the error rather than returning a schema."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/server‑error.json\")"
            }
          ],
          "expected": "JsonSchemaException (or IOException) indicating HTTP 500 Internal Server Error",
          "description": "A remote server error should be propagated as a JsonSchemaException/IOException."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/html‑response.json\")"
            }
          ],
          "expected": "JsonSchemaException (or IOException) because the response body is not valid JSON",
          "description": "The HTTP resource returns HTML (or any non‑JSON content); parsing must fail and an exception should be thrown."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"file:/tmp/unreadable-schema.json\")"
            }
          ],
          "expected": "IOException (or JsonSchemaException) caused by AccessDeniedException",
          "description": "The file exists but the process lacks read permission; loading the schema should raise an I/O‑related exception."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(SchemaLocation) - unsupported schemes and fragments",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"mailto:user@example.com\")"
            }
          ],
          "expected": "IllegalArgumentException indicating an unsupported URI scheme",
          "description": "Only http, https, classpath and file schemes are supported; a mailto scheme must be rejected."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/valid-schema.json#definition\")"
            }
          ],
          "expected": "IllegalArgumentException because fragment identifiers are not allowed in schema locations",
          "description": "A SchemaLocation containing a URI fragment should be considered invalid for loading a whole schema."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(SchemaLocation) - invalid schema content",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/unknown-meta-schema.json\")"
            }
          ],
          "expected": "JsonSchemaException indicating an unknown $schema meta‑schema IRI",
          "description": "The schema contains a $schema value that does not correspond to any registered meta‑schema; the factory must fail."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/invalid-ref.json\")"
            }
          ],
          "expected": "JsonSchemaException because a $ref points to a non‑existent external schema",
          "description": "During loading the factory encounters a $ref that cannot be resolved; this should result in an exception."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/valid-schema.yaml\")"
            }
          ],
          "expected": "JsonSchemaException (or IOException) because the default loader expects JSON and the content is YAML",
          "description": "A schema file with a .yaml extension is supplied without specifying InputFormat; the loader should reject the unsupported format."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(SchemaLocation) - security manager restrictions",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/secure-schema.json\")"
            }
          ],
          "expected": "SecurityException indicating access to the classpath resource is denied",
          "description": "When a SecurityManager blocks access to the requested classpath resource, the factory must propagate the SecurityException."
        }
      ]
    }
  ]
}