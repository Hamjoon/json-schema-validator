{
  "tests": [
    {
      "group": "Empty schema node – minimal object",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/empty-schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns a non‑null JsonSchema whose getSchemaNode() is an empty ObjectNode and whose current URI equals the supplied schemaUri",
          "description": "An empty JSON object is a valid (though trivial) schema. The factory should create a JsonSchema instance without throwing and preserve the supplied URI."
        }
      ]
    },
    {
      "group": "Boolean schema – true literal",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"file:/tmp/boolean-schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"true\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns a non‑null JsonSchema whose getSchemaNode().isBoolean() is true and current URI equals the supplied file URI",
          "description": "Draft‑07 allows a schema to be the literal boolean true. The method must accept such a node and produce a JsonSchema that reflects the boolean schema."
        }
      ]
    },
    {
      "group": "Custom configuration – typeLoose enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/type-loose.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"integer\\\",\\\"minimum\\\":5}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig(){ { setTypeLoose(true); } }"
            }
          ],
          "expected": "returns a non‑null JsonSchema; the underlying ValidationContext contains a config where isTypeLoose() == true",
          "description": "Verifies that a non‑default configuration (typeLoose flag) is correctly stored in the ValidationContext and propagated to the created JsonSchema."
        }
      ]
    },
    {
      "group": "Both schemaUri and config null",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"string\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "returns a non‑null JsonSchema; the schema’s current URI is null (or a fallback) and the internal ValidationContext uses a default SchemaValidatorsConfig",
          "description": "Ensures the method gracefully handles both a null base URI and a null configuration, falling back to defaults without throwing."
        }
      ]
    },
    {
      "group": "Schema URI containing a fragment",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/schema.json#/definitions/foo\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"$ref\\\":\\\"#/definitions/bar\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns a non‑null JsonSchema whose getCurrentUri().toString() contains the original fragment '#/definitions/foo'",
          "description": "Checks that the factory does not strip the fragment part of the supplied URI; the fragment should be retained in the created schema’s current URI."
        }
      ]
    },
    {
      "group": "Schema URI with query parameters",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/schema.json?version=1\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns a non‑null JsonSchema whose getCurrentUri().getQuery() equals 'version=1'",
          "description": "Ensures that URIs containing query strings are preserved unchanged in the created schema."
        }
      ]
    },
    {
      "group": "$id and $schema meta‑schema handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/meta.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"$id\\\":\\\"http://example.com/meta.json\\\",\\\"$schema\\\":\\\"http://json-schema.org/draft-06/schema#\\\",\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns a non‑null JsonSchema; the schema node retains both $id and $schema fields and the factory normalizes the meta‑schema URI to the internal canonical form",
          "description": "Verifies that when a schema declares its own $id and $schema (draft‑06), the factory records them correctly and applies any internal normalization logic."
        }
      ]
    },
    {
      "group": "Unsupported keyword – should still create schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/unknown-key.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\",\\\"unknownKeyword\\\":123}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns a non‑null JsonSchema; validation of the schema itself is not performed here, so the unknown keyword does not cause an exception",
          "description": "Ensures that the factory does not validate the content of the schema during creation; unknown keywords are tolerated at this stage."
        }
      ]
    },
    {
      "group": "Array schema with items subschema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/array-schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"array\\\",\\\"items\\\":{\\\"type\\\":\\\"string\\\"}}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns a non‑null JsonSchema where getSchemaNode().path(\"items\").path(\"type\").asText() equals 'string'",
          "description": "Tests that an array schema with an embedded items subschema triggers the appropriate branch in doCreate and the items node is correctly attached."
        }
      ]
    },
    {
      "group": "Schema node missing explicit type – still creates",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/no-type.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"properties\\\":{\\\"age\\\":{\\\"type\\\":\\\"integer\\\"}}}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns a non‑null JsonSchema; the node is accepted even though the top‑level 'type' keyword is absent",
          "description": "Ensures that the factory does not require a top‑level 'type' keyword; schemas that rely on implicit object type should still be instantiated."
        }
      ]
    }
  ]
}