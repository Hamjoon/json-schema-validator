{
  "testCases": [
    {
      "group": "Successful creation of JsonSchema",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
            { "parameter": "schemaLocation", "value": "mocked SchemaLocation instance" },
            { "parameter": "evaluationPath", "value": "mocked JsonNodePath instance" },
            { "parameter": "schemaNode", "value": "mocked non‑null JsonNode (e.g., ObjectNode)" },
            { "parameter": "parent", "value": "mocked parent JsonSchema instance" },
            { "parameter": "suppressSubSchemaRetrieval", "value": false }
          ],
          "expected": "instance of JsonSchema returned (no exception)",
          "description": "All required arguments are non‑null and suppressSubSchemaRetrieval is false – the factory method should return a properly constructed JsonSchema object."
        },
        {
          "input": [
            { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
            { "parameter": "schemaLocation", "value": "mocked SchemaLocation instance" },
            { "parameter": "evaluationPath", "value": "mocked JsonNodePath instance" },
            { "parameter": "schemaNode", "value": "mocked non‑null JsonNode (e.g., ObjectNode)" },
            { "parameter": "parent", "value": "mocked parent JsonSchema instance" },
            { "parameter": "suppressSubSchemaRetrieval", "value": true }
          ],
          "expected": "instance of JsonSchema returned (no exception)",
          "description": "Same as the previous case but with suppressSubSchemaRetrieval set to true – the flag should be stored in the created object without affecting construction."
        },
        {
          "input": [
            { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
            { "parameter": "schemaLocation", "value": null },
            { "parameter": "evaluationPath", "value": null },
            { "parameter": "schemaNode", "value": "mocked non‑null JsonNode (e.g., ObjectNode)" },
            { "parameter": "parent", "value": null },
            { "parameter": "suppressSubSchemaRetrieval", "value": false }
          ],
          "expected": "instance of JsonSchema returned (no exception)",
          "description": "schemaLocation, evaluationPath and parent are optional in the current implementation – the method should still succeed and create a JsonSchema instance."
        }
      ]
    },
    {
      "group": "Failure due to null mandatory parameters",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": null },
            { "parameter": "schemaLocation", "value": "mocked SchemaLocation instance" },
            { "parameter": "evaluationPath", "value": "mocked JsonNodePath instance" },
            { "parameter": "schemaNode", "value": "mocked non‑null JsonNode" },
            { "parameter": "parent", "value": "mocked parent JsonSchema instance" },
            { "parameter": "suppressSubSchemaRetrieval", "value": false }
          ],
          "expected": "NullPointerException",
          "description": "validationContext is required for the constructor; passing null should cause a NullPointerException."
        },
        {
          "input": [
            { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
            { "parameter": "schemaLocation", "value": "mocked SchemaLocation instance" },
            { "parameter": "evaluationPath", "value": "mocked JsonNodePath instance" },
            { "parameter": "schemaNode", "value": null },
            { "parameter": "parent", "value": "mocked parent JsonSchema instance" },
            { "parameter": "suppressSubSchemaRetrieval", "value": false }
          ],
          "expected": "NullPointerException",
          "description": "schemaNode represents the JSON schema definition; a null value should trigger a NullPointerException during construction."
        }
      ]
    }
  ]
}