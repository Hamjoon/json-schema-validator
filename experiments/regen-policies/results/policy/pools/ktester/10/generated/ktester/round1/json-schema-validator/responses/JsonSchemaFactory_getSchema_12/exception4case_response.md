{
  "testCases": [
    {
      "group": "Unsupported URI scheme handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"ftp://example.com/schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException indicating that the URI scheme 'ftp' is not supported",
          "description": "The factory should reject schema locations with unsupported schemes and throw a clear IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Remote URL loading failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"http://nonexistent.invalid/schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "IOException (or UncheckedIOException) indicating that the remote schema could not be retrieved",
          "description": "When the supplied HTTP URI points to a host that cannot be resolved, the factory should surface the I/O failure rather than returning a schema."
        }
      ]
    },
    {
      "group": "Invalid $ref reference handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"classpath:/schemas/person-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"{\\\"$ref\\\":\\\"#/nonexistent\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "JsonSchemaException (or IllegalArgumentException) indicating that the $ref target could not be resolved",
          "description": "A $ref that points to a non‑existent definition must cause schema creation to fail with a descriptive exception."
        }
      ]
    },
    {
      "group": "Cyclic $ref detection",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"classpath:/schemas/cyclic-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"{\\\"$ref\\\":\\\"#\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "JsonSchemaException indicating a cyclic $ref reference",
          "description": "A schema that references itself directly (or indirectly) should be detected as cyclic and result in a JsonSchemaException."
        }
      ]
    },
    {
      "group": "Malformed custom meta‑schema URI in config",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"classpath:/schemas/person-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"{\\\"$schema\\\":\\\"urn:custom:meta\\\",\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig(){ { setCustomMetaSchemaUri(\"http://[bad]\"); } }"
            }
          ],
          "expected": "IllegalArgumentException indicating that the custom meta‑schema URI is malformed",
          "description": "When a user supplies an invalid URI via setCustomMetaSchemaUri, the factory must reject it with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Null custom meta‑schema URI in config",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"classpath:/schemas/person-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"{\\\"$schema\\\":\\\"urn:custom:meta\\\",\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig(){ { setCustomMetaSchemaUri(null); } }"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) indicating that custom meta‑schema URI must not be null",
          "description": "The configuration should validate that a custom meta‑schema URI, if set, is non‑null."
        }
      ]
    },
    {
      "group": "Invalid configuration values",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"classpath:/schemas/person-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig(){ { setMaxDepth(-1); } }"
            }
          ],
          "expected": "IllegalArgumentException indicating that maxDepth must be positive",
          "description": "SchemaValidatorsConfig should reject nonsensical values such as a negative maxDepth, throwing an IllegalArgumentException during schema creation."
        }
      ]
    }
  ]
}