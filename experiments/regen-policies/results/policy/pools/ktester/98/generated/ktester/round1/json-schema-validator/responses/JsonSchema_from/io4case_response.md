{
  "test_cases": [
    {
      "group": "JsonSchema.from - null ValidationContext handling",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "null" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"http://example.com/schema\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "JsonNodeFactory.instance.objectNode()" },
            { "parameter": "parent", "value": "null" },
            { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
          ],
          "expected": "returns a non‑null JsonSchema instance; calling getValidationContext() on it returns null",
          "description": "Verify that the factory method tolerates a null ValidationContext and stores it unchanged."
        }
      ]
    },
    {
      "group": "JsonSchema.from - null SchemaLocation handling",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "null" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "JsonNodeFactory.instance.objectNode()" },
            { "parameter": "parent", "value": "null" },
            { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
          ],
          "expected": "returns a non‑null JsonSchema instance; getId() returns null because no location was supplied",
          "description": "Ensure that a null SchemaLocation does not cause a failure and that the schema’s identifier is null."
        }
      ]
    },
    {
      "group": "JsonSchema.from - null JsonNodePath handling",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"http://example.com/schema\")" },
            { "parameter": "evaluationPath", "value": "null" },
            { "parameter": "schemaNode", "value": "JsonNodeFactory.instance.objectNode()" },
            { "parameter": "parent", "value": "null" },
            { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
          ],
          "expected": "returns a non‑null JsonSchema instance; no exception is thrown during construction",
          "description": "Validate that a null evaluationPath is accepted (the path is only used for error reporting) and does not break object creation."
        }
      ]
    },
    {
      "group": "JsonSchema.from - null schemaNode handling",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"http://example.com/schema\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "null" },
            { "parameter": "parent", "value": "null" },
            { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
          ],
          "expected": "throws NullPointerException",
          "description": "The constructor expects a non‑null JsonNode representing the schema; passing null should raise NPE."
        }
      ]
    },
    {
      "group": "JsonSchema.from - schemaNode as an array node",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"http://example.com/schema\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "JsonNodeFactory.instance.arrayNode()" },
            { "parameter": "parent", "value": "null" },
            { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
          ],
          "expected": "returns a non‑null JsonSchema instance; the instance can be used in validation without error",
          "description": "Confirm that the factory method works when the supplied schema node is an array (e.g., a schema defined as a JSON array)."
        }
      ]
    },
    {
      "group": "JsonSchema.from - schemaNode as a boolean node",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"http://example.com/schema\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "JsonNodeFactory.instance.booleanNode(true)" },
            { "parameter": "parent", "value": "null" },
            { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
          ],
          "expected": "returns a non‑null JsonSchema instance; the instance can be used in validation without error",
          "description": "Validate that a primitive schema node (boolean) does not prevent object creation."
        }
      ]
    },
    {
      "group": "JsonSchema.from - parent supplied, suppress flag false",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"http://example.com/parent\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"#parent\")" },
            { "parameter": "schemaNode", "value": "JsonNodeFactory.instance.objectNode()" },
            { "parameter": "parent", "value": "new JsonSchema(new ValidationContext(), new SchemaLocation(\"http://example.com/root\"), new JsonNodePath(\"$\"), JsonNodeFactory.instance.objectNode(), null, false)" },
            { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
          ],
          "expected": "returns a non‑null JsonSchema instance; findAncestor() on the child returns the supplied parent schema",
          "description": "When a parent schema is provided, the newly created schema should retain a reference to it; this is verified via findAncestor()."
        }
      ]
    },
    {
      "group": "JsonSchema.from - parent supplied, suppress flag true",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"http://example.com/child\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"#child\")" },
            { "parameter": "schemaNode", "value": "JsonNodeFactory.instance.objectNode()" },
            { "parameter": "parent", "value": "new JsonSchema(new ValidationContext(), new SchemaLocation(\"http://example.com/root\"), new JsonNodePath(\"$\"), JsonNodeFactory.instance.objectNode(), null, false)" },
            { "parameter": "suppressSubSchemaRetrieval", "value": "true" }
          ],
          "expected": "returns a non‑null JsonSchema instance; calling getSubSchema(new JsonNodePath(\"#/any\")) on the child returns null because sub‑schema retrieval is suppressed",
          "description": "Even when a parent is present, setting suppressSubSchemaRetrieval to true must prevent the child from resolving any sub‑schemas."
        }
      ]
    }
  ]
}