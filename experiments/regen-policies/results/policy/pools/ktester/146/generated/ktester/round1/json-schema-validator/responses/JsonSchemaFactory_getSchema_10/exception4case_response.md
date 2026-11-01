{
  "test_cases": [
    {
      "group": "Unsupported URI scheme",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"ftp://example.com/schemas/schema.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws IllegalArgumentException stating that the URI scheme 'ftp' is not supported by JsonSchemaFactory.",
          "description": "Ensures that a schema location using an unsupported protocol (ftp) is rejected early with a clear exception."
        }
      ]
    },
    {
      "group": "Directory path handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws IOException (or JsonSchemaException) because the resolved resource points to a directory rather than a readable schema file.",
          "description": "Verifies that providing a SchemaLocation that resolves to a directory results in a loading failure rather than an obscure null pointer."
        }
      ]
    },
    {
      "group": "Missing internal $ref target",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/missing-internal-ref.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws JsonSchemaException indicating that the $ref '#/definitions/NonExistent' could not be resolved within the same schema.",
          "description": "Tests that a $ref pointing to a non‑existent internal definition is detected and reported as a schema loading error."
        }
      ]
    },
    {
      "group": "Unknown meta‑schema IRI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/unknown-meta-schema.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws IllegalArgumentException (or JsonSchemaException) because the schema declares an unknown $schema IRI that cannot be mapped to a JsonMetaSchema.",
          "description": "Ensures that schemas referencing an unrecognised meta‑schema IRI cause a clear failure during meta‑schema resolution."
        }
      ]
    },
    {
      "group": "Remote HTTP error response handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/schemas/not‑found.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws JsonSchemaException (or IOException) because the HTTP request returns a 404 status, indicating the remote schema could not be retrieved.",
          "description": "Validates that getSchema correctly propagates HTTP error codes (e.g., 404) as exceptions rather than silently returning a null schema."
        }
      ]
    },
    {
      "group": "Excessive recursion depth (stack overflow protection)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/deep‑recursive‑ref.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setFailFast(true)"
            }
          ],
          "expected": "Throws JsonSchemaException (or a custom RecursionDepthExceededException) before a StackOverflowError occurs, indicating that the factory detects and aborts overly deep $ref recursion.",
          "description": "Confirms that the implementation has a safeguard against unbounded $ref recursion and fails gracefully with a meaningful exception."
        }
      ]
    },
    {
      "group": "Fragment‑only schema location",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"#\\/definitions\\/MyDef\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws IllegalArgumentException because a fragment‑only location cannot be resolved without a base schema document.",
          "description": "Ensures that a SchemaLocation consisting solely of a fragment is rejected, as the factory requires a resolvable base URI to load the schema."
        }
      ]
    }
  ]
}