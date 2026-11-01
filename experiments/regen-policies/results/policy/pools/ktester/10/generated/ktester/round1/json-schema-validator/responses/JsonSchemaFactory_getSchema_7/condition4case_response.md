{
  "group": "JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig) - normal execution",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "ByteArrayInputStream containing '{\"type\":\"object\"}'"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "non‑null JsonSchema instance (schemaNode.type == \"object\")",
      "description": "Valid JSON schema is read from the InputStream and a JsonSchema object is created using a non‑null configuration."
    },
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "ByteArrayInputStream containing '{\"type\":\"string\"}'"
        },
        {
          "parameter": "config",
          "value": "null"
        }
      ],
      "expected": "non‑null JsonSchema instance (schemaNode.type == \"string\")",
      "description": "Valid JSON schema is read from the InputStream and a JsonSchema object is created when the configuration argument is null."
    }
  ]
}