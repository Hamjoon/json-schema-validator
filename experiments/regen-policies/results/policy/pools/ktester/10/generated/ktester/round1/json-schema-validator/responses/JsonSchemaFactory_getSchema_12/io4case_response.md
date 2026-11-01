{
  "testCases": [
    {
      "group": "Null arguments validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) indicating that schemaUri must not be null",
          "description": "When the schema location is null the factory cannot resolve a schema; the method should reject the call with a clear exception."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"classpath:/schemas/person-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) indicating that jsonNode must not be null",
          "description": "A null JsonNode means there is no schema definition to parse; the method should fail fast with an appropriate exception."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            },
            {
              "parameter": "jsonNode",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) for the first null argument encountered",
          "description": "All three arguments are null – the method must guard against nulls and throw a predictable exception."
        }
      ]
    },
    {
      "group": "Invalid SchemaLocation handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"http://[invalid-uri]\")"
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
          "expected": "IllegalArgumentException or IOException indicating that the URI is malformed or cannot be accessed",
          "description": "A malformed URI should be detected early and result in an exception rather than an undefined JsonSchema."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"classpath:/schemas/non‑existent.json\")"
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
          "expected": "IOException (or a custom SchemaLoadingException) because the classpath resource cannot be found",
          "description": "When the supplied classpath location does not exist the factory should surface the loading failure."
        }
      ]
    },
    {
      "group": "Invalid JsonNode content",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"classpath:/schemas/person-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"[1,2,3]\")"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException indicating that the root node must be an OBJECT node",
          "description": "A JSON schema must be represented by an object node; providing an array should cause validation to reject the input."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"classpath:/schemas/person-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"{\\\"notAValidKeyword\\\":true}\")"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "JsonSchema instance (schema parsing succeeds) but later validation of data will fail for unknown keywords",
          "description": "The method should still create a JsonSchema even if the node contains unknown keywords; schema creation does not enforce strict keyword validation."
        }
      ]
    },
    {
      "group": "Custom SchemaValidatorsConfig propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"classpath:/schemas/person-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"age\\\":{\\\"type\\\":\\\"integer\\\"}}}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig(){ { setTypeLoose(true); } }"
            }
          ],
          "expected": "non‑null JsonSchema instance whose ValidationContext reflects the custom config (typeLoose = true)",
          "description": "Passing a config with typeLoose enabled should be honored by the created schema; the test verifies that the returned schema is not null and that the custom flag is stored in its context."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"classpath:/schemas/person-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"email\\\":{\\\"format\\\":\\\"email\\\"}}}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig(){ { setFormatValidatorFactory(uri -> (value) -> true); } }"
            }
          ],
          "expected": "non‑null JsonSchema instance that will accept any string for the 'email' format because the custom format validator always returns true",
          "description": "Ensures that a user‑provided format validator factory is correctly injected and influences the resulting schema's validation behavior."
        }
      ]
    },
    {
      "group": "Meta‑schema resolution via $schema keyword",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"classpath:/schemas/unknown-meta-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"{\\\"$schema\\\":\\\"http://json-schema.org/draft-06/schema#\\\",\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "non‑null JsonSchema instance whose meta‑schema is the Draft‑06 meta‑schema",
          "description": "The $schema keyword should drive meta‑schema selection; the factory must load the Draft‑06 meta‑schema even though the default version flag may be later."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "SchemaLocation.of(\"classpath:/schemas/custom-meta-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"{\\\"$schema\\\":\\\"urn:custom:meta\\\",\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig(){ { setCustomMetaSchemaUri(\"urn:custom:meta\"); } }"
            }
          ],
          "expected": "non‑null JsonSchema instance whose meta‑schema matches the custom URI supplied in the config",
          "description": "Verifies that a custom meta‑schema URI configured via SchemaValidatorsConfig is respected when the $schema keyword points to it."
        }
      ]
    }
  ]
}