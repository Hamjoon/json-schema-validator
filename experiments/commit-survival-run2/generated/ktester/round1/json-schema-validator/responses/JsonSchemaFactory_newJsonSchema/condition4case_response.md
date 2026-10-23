{
  "testCases": [
    {
      "group": "Valid inputs – typical JSON schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"name\\\":{\\\"type\\\":\\\"string\\\"}}}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns a non‑null JsonSchema instance whose getSchemaNode() equals the supplied schemaNode and whose getCurrentUri() equals the supplied schemaUri",
          "description": "Happy‑path test: a well‑formed JSON schema, a valid HTTP URI and a non‑null configuration should result in a successfully created JsonSchema object."
        }
      ]
    },
    {
      "group": "Null configuration – default behaviour",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"file:/tmp/meta-schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"$schema\\\":\\\"http://json-schema.org/draft-07/schema#\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "returns a non‑null JsonSchema instance; the internal ValidationContext should contain a default SchemaValidatorsConfig (i.e., config field is not null)",
          "description": "When the config argument is null the method must still create a JsonSchema using the default configuration."
        }
      ]
    },
    {
      "group": "Null schema URI – internal handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"array\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns a non‑null JsonSchema instance whose current URI is null (or a fallback URI as defined by the implementation)",
          "description": "The factory should accept a null base URI and still create a schema; this verifies that the method does not throw NPE when schemaUri is null."
        }
      ]
    },
    {
      "group": "Null schema node – error handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/empty.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "throws NullPointerException",
          "description": "createValidationContext is expected to require a non‑null JsonNode; passing null should result in an NPE."
        }
      ]
    },
    {
      "group": "Complex schema with nested subschemas – branch coverage in doCreate",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/complex.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"address\\\":{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"street\\\":{\\\"type\\\":\\\"string\\\"}}}}}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns a non‑null JsonSchema where nested subschema objects are correctly created (e.g., schema.getSchemaNode().path(\"properties\").path(\"address\").has(\"properties\"))",
          "description": "A schema containing nested objects forces doCreate to traverse multiple branches (object, properties, subschema retrieval). This test ensures those internal branches are exercised."
        }
      ]
    },
    {
      "group": "Schema with $ref – URI translation branch",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/main.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"$ref\\\":\\\"definitions.json#/def\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns a JsonSchema whose internal reference has been resolved via the URITranslator (i.e., the resulting schema is not a raw $ref node)",
          "description": "When a schema contains a $ref, doCreate will invoke the URI translator and follow a different code path. This test validates that the translation occurs without throwing."
        }
      ]
    }
  ]
}