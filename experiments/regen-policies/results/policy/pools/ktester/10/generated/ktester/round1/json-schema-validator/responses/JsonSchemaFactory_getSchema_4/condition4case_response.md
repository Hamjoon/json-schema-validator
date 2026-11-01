{
  "testCases": [
    {
      "group": "getSchema(SchemaLocation) - null handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException (or NullPointerException) is thrown because the schema location argument is null",
          "description": "Verify that calling getSchema with a null SchemaLocation fails fast with an appropriate exception."
        }
      ]
    },
    {
      "group": "getSchema(SchemaLocation) - valid classpath resource",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/simple-schema.json\")"
            }
          ],
          "expected": "JsonSchema instance is returned and its meta‑schema version matches the factory's default version",
          "description": "Load a well‑formed JSON schema that lives on the classpath. The method should return a non‑null JsonSchema object."
        }
      ]
    },
    {
      "group": "getSchema(SchemaLocation) - valid file system resource",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"file:/tmp/test-schema.json\")"
            }
          ],
          "expected": "JsonSchema instance is returned and can be used for validation",
          "description": "Load a schema from an absolute file URI. The method should resolve the file, parse it and return a JsonSchema."
        }
      ]
    },
    {
      "group": "getSchema(SchemaLocation) - non‑existent resource",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/missing-schema.json\")"
            }
          ],
          "expected": "JsonSchemaNotFoundException (or a RuntimeException indicating the schema could not be located) is thrown",
          "description": "When the supplied SchemaLocation points to a resource that cannot be found, the factory should raise an exception rather than returning a null schema."
        }
      ]
    },
    {
      "group": "getSchema(SchemaLocation) - malformed JSON schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/malformed-schema.json\")"
            }
          ],
          "expected": "JsonSchemaParseException (or a JsonProcessingException wrapped in a RuntimeException) is thrown",
          "description": "If the schema file exists but contains invalid JSON, the method must propagate a parsing error."
        }
      ]
    },
    {
      "group": "getSchema(SchemaLocation) - meta‑schema resolution branch",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/schema-with-$ref.json\")"
            }
          ],
          "expected": "JsonSchema instance is returned and its $ref references are resolved using the factory's meta‑schema loader",
          "description": "A schema that contains $ref to another schema triggers the internal branch that loads referenced schemas. The test ensures the reference is resolved and no exception is thrown."
        }
      ]
    },
    {
      "group": "getSchema(SchemaLocation) - custom configuration via overloaded method (indirect branch)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/simple-schema.json\")"
            }
          ],
          "expected": "JsonSchema instance is returned; the instance reflects the default SchemaValidatorsConfig created inside the method",
          "description": "The one‑parameter overload delegates to getSchema(schemaUri, createSchemaValidatorsConfig()). This test confirms that the delegation occurs and the returned schema is built with the default configuration."
        }
      ]
    }
  ]
}