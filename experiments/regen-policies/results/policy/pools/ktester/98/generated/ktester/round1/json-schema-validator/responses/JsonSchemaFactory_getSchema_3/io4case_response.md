{
  "groups": [
    {
      "group": "Boolean Schemas",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "true"
            }
          ],
          "expected": "A non‑null JsonSchema instance that always validates any JSON instance successfully (boolean schema true).",
          "description": "Pass a boolean true as the schema root. According to the JSON‑Schema specification a boolean true represents a schema that matches everything. The factory should return a JsonSchema that never produces validation errors."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "false"
            }
          ],
          "expected": "A non‑null JsonSchema instance that always fails validation (boolean schema false).",
          "description": "Pass a boolean false as the schema root. This represents a schema that matches no instance. The factory should create a JsonSchema that reports a validation error for any input."
        }
      ]
    },
    {
      "group": "Advanced Meta‑schema Versions",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"http://json-schema.org/draft-06/schema#\", \"type\": \"boolean\" }"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose meta‑schema URI is \"http://json-schema.org/draft-06/schema#\" (Draft‑06). The schema validates boolean values.",
          "description": "Supply a schema that explicitly requests Draft‑06 via $schema. The factory must recognise the URI, load the Draft‑06 meta‑schema and bind the resulting JsonSchema to it."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"https://json-schema.org/draft/2019-09/schema\", \"type\": \"null\" }"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose meta‑schema URI is \"https://json-schema.org/draft/2019-09/schema\" (Draft‑2019‑09). The schema validates null values.",
          "description": "Provide a schema that targets the newer Draft‑2019‑09 meta‑schema. The factory should correctly resolve this URI and create a JsonSchema using that meta‑schema."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\", \"type\": \"array\", \"items\": { \"type\": \"integer\" } }"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose meta‑schema URI is \"https://json-schema.org/draft/2020-12/schema\" (Draft‑2020‑12). The schema validates arrays of integers.",
          "description": "Test the factory with the most recent Draft‑2020‑12 meta‑schema. The expected behavior is that the factory loads this meta‑schema and returns a JsonSchema bound to it."
        }
      ]
    },
    {
      "group": "Schema with $ref and $id",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$id\": \"http://example.com/root\", \"$defs\": { \"positiveInt\": { \"type\": \"integer\", \"minimum\": 1 } }, \"$ref\": \"#/$defs/positiveInt\" }"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose meta‑schema is the default (Draft‑07) and that resolves the internal $ref successfully; validation of the integer 5 succeeds, validation of 0 fails.",
          "description": "Create a schema that defines an internal definition via $defs and references it with $ref. The factory must build a JsonSchema capable of resolving the reference. The test asserts that the resulting schema behaves as expected for valid and invalid instances."
        }
      ]
    },
    {
      "group": "Invalid Root Types (non‑object, non‑boolean)",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "\"just a string\""
            }
          ],
          "expected": "JsonSchemaException (or IllegalArgumentException) indicating that the root node must be an object, array, or boolean schema, not a JSON string.",
          "description": "Pass a JSON string as the schema root. The factory should reject this as an invalid schema representation."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "12345"
            }
          ],
          "expected": "JsonSchemaException (or IllegalArgumentException) indicating that the root node must be an object, array, or boolean schema, not a numeric literal.",
          "description": "Pass a JSON number as the schema root. The method must fail with an appropriate exception."
        }
      ]
    },
    {
      "group": "Empty or Null $schema Values",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"\", \"type\": \"string\" }"
            }
          ],
          "expected": "A non‑null JsonSchema instance that falls back to the default meta‑schema (Draft‑07) because the $schema value is empty.",
          "description": "Provide an empty string for $schema. The factory should treat it as unspecified and use the default meta‑schema."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": null, \"type\": \"string\" }"
            }
          ],
          "expected": "A non‑null JsonSchema instance that falls back to the default meta‑schema (Draft‑07) because the $schema value is null.",
          "description": "Supply a null $schema value. The factory must ignore the null and default to Draft‑07."
        }
      ]
    },
    {
      "group": "Schema with Unknown Keywords",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"type\": \"object\", \"unknownKeyword\": { \"foo\": \"bar\" }, \"properties\": { \"id\": { \"type\": \"string\" } } }"
            }
          ],
          "expected": "A non‑null JsonSchema instance that successfully parses the schema, ignoring the unknownKeyword, and validates objects with a string \"id\" property.",
          "description": "Include a custom, unknown keyword in the schema. The factory should not fail; it must ignore unknown keywords per the specification and still produce a usable JsonSchema."
        }
      ]
    }
  ]
}