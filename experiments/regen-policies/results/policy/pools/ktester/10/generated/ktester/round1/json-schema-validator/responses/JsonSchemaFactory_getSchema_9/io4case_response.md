{
  "testCases": [
    {
      "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode) - complex object node",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"file:///tmp/schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonNodeFactory.instance.objectNode()\n    .put(\"type\", \"object\")\n    .set(\"properties\", JsonNodeFactory.instance.objectNode()\n        .set(\"name\", JsonNodeFactory.instance.objectNode().put(\"type\", \"string\"))\n        .set(\"age\", JsonNodeFactory.instance.objectNode().put(\"type\", \"integer\")))"
            }
          ],
          "expected": "non‑null JsonSchema instance; newJsonSchema is invoked with the *same* SchemaLocation instance, the *same* JsonNode instance (reference equality), and a null config",
          "description": "Pass a valid file‑scheme SchemaLocation and a richly nested JsonNode. The method should delegate unchanged to newJsonSchema and return the JsonSchema produced by the overridden newJsonSchema implementation."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode) - NullNode as jsonNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/nullnode.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonNodeFactory.instance.nullNode()"
            }
          ],
          "expected": "non‑null JsonSchema instance; newJsonSchema receives the original SchemaLocation and the NullNode instance, config is null",
          "description": "Jackson represents JSON literal null with a NullNode object. Even though the node represents a null value, it is a non‑null Java reference, so getSchema should forward it unchanged."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode) - relative URI with null jsonNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"schemas/subschema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "null"
            }
          ],
          "expected": "non‑null JsonSchema instance; newJsonSchema is called with the relative SchemaLocation, null jsonNode, and null config",
          "description": "Validate that a relative URI is treated the same as an absolute one – the method must not alter or reject it and must still delegate correctly."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode) - empty object node",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"http://example.com/empty.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            }
          ],
          "expected": "non‑null JsonSchema instance; newJsonSchema receives the original SchemaLocation, the empty ObjectNode instance, and null config",
          "description": "An empty JSON object should be passed through unchanged. The test confirms that the factory does not attempt to interpret or modify the node before delegating."
        }
      ]
    }
  ]
}