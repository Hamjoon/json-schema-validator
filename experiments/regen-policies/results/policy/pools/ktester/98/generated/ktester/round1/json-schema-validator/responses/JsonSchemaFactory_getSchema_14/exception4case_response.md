{
  "result": [
    {
      "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig) - exception scenarios",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"ftp://example.com/schema.json\")"
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
          "description": "The supplied SchemaLocation uses an unsupported URI scheme (ftp). JsonSchemaFactory should reject it and throw IllegalArgumentException before attempting to load the schema."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schema/example-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"[\\\"not\\\",\\\"an\\\",\\\"object\\\"]\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "jsonNode is a JSON array rather than an object. The factory expects the root of a JSON Schema to be an object node; it should raise IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schema/example-schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "new ObjectMapper().readTree(\"{\\\"$schema\\\":\\\"http://invalid-meta-schema.org/draft-07/schema#\\\",\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "SchemaLoadingException",
          "description": "The schema contains a $schema reference that points to a non‑existent or unsupported meta‑schema URI. During creation the factory attempts to load the meta‑schema and should propagate a SchemaLoadingException."
        }
      ]
    }
  ]
}