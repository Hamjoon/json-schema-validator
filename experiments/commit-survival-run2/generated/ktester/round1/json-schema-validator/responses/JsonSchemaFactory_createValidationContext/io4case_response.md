{
  "tests": [
    {
      "group": "$schema keyword with non‑string value",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"$schema\": 123, \"type\": \"object\" }"
            }
          ],
          "expected": "IllegalArgumentException (or SchemaException) is thrown because the value of `$schema` is not a string.",
          "description": "The `$schema` keyword must be a string. Supplying a numeric value should cause the factory to reject the schema and raise an exception."
        }
      ]
    },
    {
      "group": "$schema keyword with surrounding whitespace",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"$schema\": \"  http://json-schema.org/draft-07/schema#  \", \"type\": \"null\" }"
            }
          ],
          "expected": "A ValidationContext is returned whose jsonMetaSchema URI is `http://json-schema.org/draft-07/schema#` (whitespace trimmed). The uriFactory and urnFactory are the same instances as in the JsonSchemaFactory under test.",
          "description": "Even if the `$schema` value contains leading/trailing spaces, the factory should trim it and resolve the known meta‑schema correctly."
        }
      ]
    },
    {
      "group": "$schema keyword without fragment identifier",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"$schema\": \"http://json-schema.org/draft-07/schema\", \"type\": \"integer\" }"
            }
          ],
          "expected": "A ValidationContext is returned whose jsonMetaSchema URI is normalized to `http://json-schema.org/draft-07/schema#` (fragment added).",
          "description": "When the `$schema` URI lacks the trailing `#`, the factory should normalise it to the canonical form that includes the fragment."
        }
      ]
    },
    {
      "group": "Factory built with SPECIFIC version flag (draft‑04) and missing $schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"type\": \"boolean\" }"
            }
          ],
          "expected": "A ValidationContext is returned whose jsonMetaSchema URI is `http://json-schema.org/draft-04/schema#`, i.e., the default meta‑schema for a factory created with `SpecVersion.VersionFlag.V4`.",
          "description": "When the JsonSchemaFactory is constructed via `JsonSchemaFactory.builder().specVersion(SpecVersion.VersionFlag.V4).build()`, a schema without an explicit `$schema` must fall back to the draft‑04 meta‑schema."
        }
      ]
    },
    {
      "group": "Multiple invocations produce independent ValidationContext objects",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"type\": \"string\" }"
            }
          ],
          "expected": "Two successive calls to `createValidationContext` with the same schema node return two distinct ValidationContext instances (different object identity).",
          "description": "The method should create a fresh ValidationContext each time; there must be no unintended caching or reuse of mutable state between calls."
        }
      ]
    },
    {
      "group": "$schema keyword as JSON array (invalid type)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"$schema\": [\"http://json-schema.org/draft-07/schema#\"], \"type\": \"object\" }"
            }
          ],
          "expected": "IllegalArgumentException (or SchemaException) is thrown because `$schema` is an array instead of a string.",
          "description": "The factory must validate the type of the `$schema` keyword; an array value is illegal and should cause an exception."
        }
      ]
    },
    {
      "group": "$schema keyword as boolean true (invalid type)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "{ \"$schema\": true, \"type\": \"number\" }"
            }
          ],
          "expected": "IllegalArgumentException (or SchemaException) is thrown because `$schema` is a boolean, not a string.",
          "description": "Ensures that non‑string primitive values for `$schema` are rejected with a clear error."
        }
      ]
    }
  ]
}