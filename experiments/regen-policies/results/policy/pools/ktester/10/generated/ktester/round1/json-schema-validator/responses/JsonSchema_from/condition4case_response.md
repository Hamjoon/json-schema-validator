{
  "testCases": [
    {
      "group": "Construction with all non‑null arguments (suppressSubSchemaRetrieval = false)",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schema.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "JsonMapperFactory.getInstance().readTree(\"{\\\"type\\\":\\\"object\\\"}\")" },
            { "parameter": "parent", "value": "null" },
            { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
          ],
          "expected": "A non‑null JsonSchema instance whose getValidationContext() returns the supplied ValidationContext, whose getId() matches the supplied SchemaLocation, whose findAncestor() returns null (no parent), and whose internal flag for suppressSubSchemaRetrieval is false.",
          "description": "Verifies that the static factory creates a proper JsonSchema object when all required arguments are provided and the suppress flag is false."
        }
      ]
    },
    {
      "group": "Construction with parent schema (suppressSubSchemaRetrieval = true)",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/child-schema.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$.child\")" },
            { "parameter": "schemaNode", "value": "JsonMapperFactory.getInstance().readTree(\"{\\\"type\\\":\\\"string\\\"}\")" },
            { "parameter": "parent", "value": "JsonSchema.from(new ValidationContext(), new SchemaLocation(\"classpath:/parent-schema.json\"), new JsonNodePath(\"$\"), JsonMapperFactory.getInstance().readTree(\"{\\\"type\\\":\\\"object\\\"}\"), null, false)" },
            { "parameter": "suppressSubSchemaRetrieval", "value": "true" }
          ],
          "expected": "A non‑null JsonSchema instance whose findAncestor() returns the supplied parent schema and whose internal suppressSubSchemaRetrieval flag is true.",
          "description": "Ensures that when a parent JsonSchema is supplied, the created schema correctly records the ancestor relationship and respects the suppress flag."
        }
      ]
    },
    {
      "group": "Construction with null ValidationContext (should throw NullPointerException)",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "null" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/schema.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "JsonMapperFactory.getInstance().readTree(\"{\\\"type\\\":\\\"object\\\"}\")" },
            { "parameter": "parent", "value": "null" },
            { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
          ],
          "expected": "NullPointerException",
          "description": "The factory method is expected to validate mandatory arguments; passing a null ValidationContext must result in NPE."
        }
      ]
    },
    {
      "group": "Construction with empty schema node (object node with no properties)",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/empty-schema.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "JsonMapperFactory.getInstance().readTree(\"{}\")" },
            { "parameter": "parent", "value": "null" },
            { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
          ],
          "expected": "A non‑null JsonSchema instance; subsequent calls to getValidators() should return an empty list because the schema node contains no validation keywords.",
          "description": "Checks that the factory can handle an empty JSON schema definition without failing."
        }
      ]
    },
    {
      "group": "Construction with malformed schema JSON (invalid JSON string)",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "new ValidationContext()" },
            { "parameter": "schemaLocation", "value": "new SchemaLocation(\"classpath:/bad-schema.json\")" },
            { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
            { "parameter": "schemaNode", "value": "null /* JsonNode parsing failed, resulting in null */" },
            { "parameter": "parent", "value": "null" },
            { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
          ],
          "expected": "IllegalArgumentException",
          "description": "If the schemaNode cannot be parsed (null), the factory should reject the input and throw an IllegalArgumentException."
        }
      ]
    }
  ]
}