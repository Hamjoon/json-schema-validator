{
  "test_cases": [
    {
      "group": "getSchema(SchemaLocation) - invalid URI syntax",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"ht!tp://[invalid]\")"
            }
          ],
          "expected": "IllegalArgumentException (or URISyntaxException wrapped in RuntimeException) is thrown because the supplied URI string cannot be parsed",
          "description": "Verify that a malformed URI string inside SchemaLocation causes the factory to fail fast with an appropriate exception."
        }
      ]
    },
    {
      "group": "getSchema(SchemaLocation) - directory path instead of file",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"file:/tmp/\" )"
            }
          ],
          "expected": "JsonSchemaNotFoundException (or IOException) is thrown because the location resolves to a directory, not a readable schema file",
          "description": "Ensure that pointing to a directory rather than a concrete schema file results in an error rather than an unexpected JsonSchema instance."
        }
      ]
    },
    {
      "group": "getSchema(SchemaLocation) - non‑JSON/YAML content",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/not-a-schema.txt\")"
            }
          ],
          "expected": "JsonProcessingException (wrapped in JsonSchemaException or RuntimeException) is thrown because the content cannot be parsed as JSON/YAML",
          "description": "Load a resource that exists but contains plain text; the factory should attempt to parse it and fail with a parsing exception."
        }
      ]
    },
    {
      "group": "getSchema(SchemaLocation) - cyclic $ref detection",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/cyclic-ref-schema.json\")"
            }
          ],
          "expected": "JsonSchemaException (or IllegalStateException) is thrown indicating a reference cycle was detected",
          "description": "Validate that the factory detects a $ref loop (schema A -> B -> A) and aborts with a clear cycle‑detection exception."
        }
      ]
    },
    {
      "group": "getSchema(SchemaLocation) - unsupported meta‑schema version",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/unsupported-meta-schema.json\")"
            }
          ],
          "expected": "JsonSchemaException (or IllegalArgumentException) is thrown because the $schema value refers to a meta‑schema version that the factory does not recognize",
          "description": "Load a schema that declares an unknown $schema URI; the factory should reject it rather than silently defaulting."
        }
      ]
    },
    {
      "group": "getSchema(SchemaLocation) - file permission I/O error",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"file:/root/secret-schema.json\")"
            }
          ],
          "expected": "IOException (wrapped in JsonSchemaException) is thrown because the file cannot be read due to insufficient permissions",
          "description": "Attempt to load a schema file that exists but is not readable by the process, verifying that the factory propagates the I/O failure."
        }
      ]
    },
    {
      "group": "getSchema(SchemaLocation) - unsupported jar scheme",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"jar:file:/app.jar!/schemas/inside.jar-schema.json\")"
            }
          ],
          "expected": "IllegalArgumentException is thrown because the \"jar\" URI scheme is not supported by JsonSchemaFactory",
          "description": "Confirm that a jar‑based URI is rejected with a clear unsupported‑scheme exception."
        }
      ]
    },
    {
      "group": "getSchema(SchemaLocation) - HTTPS resource with SSL handshake failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"https://untrusted.example.com/bad-schema.json\")"
            }
          ],
          "expected": "RuntimeException (e.g., SSLHandshakeException wrapped) is thrown when the factory attempts to fetch the schema over HTTPS and the SSL validation fails",
          "description": "Simulate an HTTPS endpoint with an invalid certificate to verify that network‑level SSL errors are surfaced as exceptions."
        }
      ]
    },
    {
      "group": "getSchema(SchemaLocation) - internal null path in SchemaLocation",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(null)"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) is thrown because the underlying location string is null",
          "description": "Directly construct a SchemaLocation with a null string to ensure the factory validates the internal state before proceeding."
        }
      ]
    }
  ]
}