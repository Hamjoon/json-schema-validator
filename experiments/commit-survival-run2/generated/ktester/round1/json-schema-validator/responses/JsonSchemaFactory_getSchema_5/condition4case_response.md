{
  "testCases": [
    {
      "group": "Valid URI - schema loaded from classpath",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"classpath:/schemas/example-schema.json\")"
            }
          ],
          "expected": "Returns a non‑null JsonSchema instance representing the parsed schema.",
          "description": "A well‑formed URI that points to an existing JSON schema file on the classpath. The method should load the resource, parse it and return a JsonSchema object."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"file:/tmp/example-schema.json\")"
            }
          ],
          "expected": "Returns a non‑null JsonSchema instance representing the parsed schema.",
          "description": "A file‑system URI that points to a readable JSON schema file. The method should resolve the file, read its contents and return a JsonSchema."
        }
      ]
    },
    {
      "group": "Valid URI - schema loaded from HTTP(S)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"https://raw.githubusercontent.com/networknt/json-schema-validator/master/src/test/resources/schema/draftv4/schema.json\")"
            }
          ],
          "expected": "Returns a non‑null JsonSchema instance representing the parsed remote schema.",
          "description": "A remote HTTP(S) URI that resolves to a valid JSON schema. The method should use the URITranslator to fetch the content and build the JsonSchema."
        }
      ]
    },
    {
      "group": "Null URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            }
          ],
          "expected": "Throws IllegalArgumentException with message containing \"schemaUri must not be null\".",
          "description": "The method must guard against a null argument and fail fast with an appropriate exception."
        }
      ]
    },
    {
      "group": "Unsupported scheme",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"ftp://example.com/schema.json\")"
            }
          ],
          "expected": "Throws JsonSchemaException (or RuntimeException) indicating that the scheme 'ftp' is not supported.",
          "description": "The URI uses a scheme that the factory does not know how to translate. The method should detect this and raise an error."
        }
      ]
    },
    {
      "group": "URI with fragment identifier",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"classpath:/schemas/complex-schema.json#definitions/address\")"
            }
          ],
          "expected": "Returns a JsonSchema that represents the subschema located at the fragment (e.g., definitions/address).",
          "description": "When a fragment is present, the factory should resolve the base document first and then locate the subschema identified by the fragment."
        }
      ]
    },
    {
      "group": "URI points to non‑existent resource",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"classpath:/schemas/missing-schema.json\")"
            }
          ],
          "expected": "Throws JsonSchemaException (or IOException) indicating that the resource could not be found.",
          "description": "The URI resolves to a location that does not contain a schema file. The method should attempt to load it and fail with a clear error."
        }
      ]
    },
    {
      "group": "Meta‑schema URI normalization",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"http://json-schema.org/draft-04/schema#\")"
            }
          ],
          "expected": "Returns the built‑in Draft‑04 JsonSchema instance (cached) rather than loading from network.",
          "description": "The factory normalizes known meta‑schema URIs and returns the pre‑registered schema instead of performing a remote fetch."
        }
      ]
    },
    {
      "group": "URI with query parameters (should be ignored)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "java.net.URI.create(\"classpath:/schemas/example-schema.json?version=1\")"
            }
          ],
          "expected": "Returns a JsonSchema identical to loading the same URI without the query string.",
          "description": "The implementation should strip query parameters before locating the resource, ensuring deterministic loading."
        }
      ]
    }
  ]
}