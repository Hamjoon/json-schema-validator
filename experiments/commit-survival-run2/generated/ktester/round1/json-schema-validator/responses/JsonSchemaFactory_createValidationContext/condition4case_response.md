{
  "testCases": [
    {
      "group": "Schema node with explicit $schema (known meta‑schema)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"\\$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"object\" }"
            }
          ],
          "expected": "A ValidationContext is returned whose jsonMetaSchema has the URI `http://json-schema.org/draft-07/schema#`. The context’s uriFactory and urnFactory are the same instances that belong to the JsonSchemaFactory used in the test.",
          "description": "When the schema node contains a `$schema` keyword that matches a known meta‑schema, `findMetaSchemaForSchema` should resolve that meta‑schema and the created ValidationContext must contain it."
        }
      ]
    },
    {
      "group": "Schema node without $schema (default meta‑schema)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"type\": \"string\" }"
            }
          ],
          "expected": "A ValidationContext is returned whose jsonMetaSchema corresponds to the factory’s default meta‑schema (e.g., `http://json-schema.org/draft-07/schema#`). No exception is thrown.",
          "description": "If the schema node does not specify a `$schema` keyword, the factory should fall back to its default meta‑schema when creating the ValidationContext."
        }
      ]
    },
    {
      "group": "Null schema node",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "null"
            }
          ],
          "expected": "NullPointerException is thrown (or IllegalArgumentException depending on implementation) because the method requires a non‑null JsonNode.",
          "description": "The method should validate its argument and reject a null schema node, ensuring callers cannot create a ValidationContext from a null reference."
        }
      ]
    },
    {
      "group": "Schema node with unknown $schema URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"\\$schema\": \"http://example.com/unknown/schema#\", \"type\": \"array\" }"
            }
          ],
          "expected": "IllegalArgumentException (or a custom SchemaException) is thrown indicating that the meta‑schema cannot be found.",
          "description": "When `$schema` points to an URI that the factory does not recognise, `findMetaSchemaForSchema` should raise an error, and consequently `createValidationContext` should not return a ValidationContext."
        }
      ]
    },
    {
      "group": "Schema node with $schema pointing to a known meta‑schema that requires URI translation",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"\\$schema\": \"urn:jsonschema:draft-07\", \"type\": \"number\" }"
            }
          ],
          "expected": "A ValidationContext is returned whose jsonMetaSchema URI is the normalized form `http://json-schema.org/draft-07/schema#` (translation performed by the URITranslator).",
          "description": "The factory may support URN‑based meta‑schema identifiers; this test verifies that the URITranslator correctly normalises the URN to the standard HTTP URI before constructing the ValidationContext."
        }
      ]
    }
  ]
}