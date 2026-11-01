{
  "test_cases": [
    {
      "group": "Invalid JsonNode types",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/array-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"[]\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The schema node is a JSON array, which is not a valid schema object. The factory should reject it and throw IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/number-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"42\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The schema node is a JSON numeric literal, which is illegal for a schema definition. The method must throw IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/boolean-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"true\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The schema node is a JSON boolean literal. Since a schema must be an object, the factory should raise IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Unsupported URI schemes",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"data:application/json,{\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "A data URI is not supported by JsonSchemaFactory for locating schemas; the method should reject it with IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"gopher://example.com/schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"string\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The Gopher protocol is not a recognized schema location scheme; the factory must throw IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Malformed URI components",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/invalid%ZZpath.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The URI contains an invalid percent‑encoding sequence (%ZZ). The factory should detect the malformed encoding and throw IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com\")"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The URI lacks a path component that points to a schema file. JsonSchemaFactory expects a concrete schema location and should reject a bare authority URI with IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/../schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The URI contains path traversal segments (\"..\") which are considered unsafe for schema locations. The factory should reject it with IllegalArgumentException."
        }
      ]
    }
  ]
}