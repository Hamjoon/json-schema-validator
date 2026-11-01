{
  "testCases": [
    {
      "group": "Complex schema node with definitions and $ref",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/complex.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonMapperFactory.getMapper().readTree(\"{\\\"$schema\\\":\\\"http://json-schema.org/draft-07/schema#\\\",\\\"definitions\\\":{\\\"positive\\\":{\\\"type\\\":\\\"integer\\\",\\\"minimum\\\":1}},\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"age\\\":{\\\"$ref\\\":\\\"#/definitions/positive\\\"}}}\")"
            }
          ],
          "expected": "instance of JsonSchema whose meta‑schema is Draft‑07, location equals \"http://example.com/complex.json\", and internal $ref resolution works (i.e., the schema contains a resolved definition for \"age\")",
          "description": "A schema containing definitions and a $ref should be loaded correctly; the factory must preserve the $ref so that later validation can resolve it."
        }
      ]
    },
    {
      "group": "URI containing query and fragment",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"https://example.org/schema.json?version=1#section2\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonMapperFactory.getMapper().readTree(\"{\\\"type\\\":\\\"string\\\",\\\"maxLength\\\":10}\")"
            }
          ],
          "expected": "instance of JsonSchema whose getSchemaLocation().toString() equals \"https://example.org/schema.json?version=1#section2\"",
          "description": "Verify that query strings and fragments are preserved in the stored SchemaLocation."
        }
      ]
    },
    {
      "group": "URI with Unicode characters (percent‑encoded)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/über.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonMapperFactory.getMapper().readTree(\"{\\\"type\\\":\\\"null\\\"}\")"
            }
          ],
          "expected": "instance of JsonSchema whose getSchemaLocation().toString() equals \"http://example.com/über.json\" (the Unicode character is correctly retained after normalization)",
          "description": "A URI containing non‑ASCII characters should survive the factory’s normalization unchanged."
        }
      ]
    },
    {
      "group": "Boolean schema node (true)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/alwaysValid.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonMapperFactory.getMapper().readTree(\"true\")"
            }
          ],
          "expected": "instance of JsonSchema representing a boolean true schema (i.e., any instance validates successfully)",
          "description": "JSON Schema draft‑07 allows a schema to be a literal boolean; the factory should accept a JsonNode that is the literal true."
        }
      ]
    },
    {
      "group": "Boolean schema node (false)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/alwaysInvalid.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonMapperFactory.getMapper().readTree(\"false\")"
            }
          ],
          "expected": "instance of JsonSchema representing a boolean false schema (i.e., no instance validates successfully)",
          "description": "A literal false schema should be accepted and reflected in the created JsonSchema."
        }
      ]
    },
    {
      "group": "Non‑object, non‑boolean schema node (string literal)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/invalidSchema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonMapperFactory.getMapper().readTree(\"\\\"just a string\\\"\")"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "A schema node that is a JSON string (neither object nor boolean) is not a valid JSON Schema; the factory should reject it with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Unknown $schema IRI – fallback to default meta‑schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/unknownMeta.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonMapperFactory.getMapper().readTree(\"{\\\"$schema\\\":\\\"http://example.com/unknown#\\\",\\\"type\\\":\\\"number\\\"}\")"
            }
          ],
          "expected": "instance of JsonSchema whose meta‑schema defaults to the library's latest supported version (e.g., Draft‑07) because the provided $schema IRI cannot be resolved",
          "description": "When the $schema keyword points to an IRI that the factory does not recognise, it should fall back to the default meta‑schema rather than throwing an exception."
        }
      ]
    }
  ]
}