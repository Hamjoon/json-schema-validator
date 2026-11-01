{
  "test_cases": [
    {
      "group": "Mixed optional nulls with suppressSubSchemaRetrieval true",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
            { "parameter": "schemaLocation", "value": null },
            { "parameter": "evaluationPath", "value": "mocked JsonNodePath instance" },
            { "parameter": "schemaNode", "value": "mocked non‑null ObjectNode" },
            { "parameter": "parent", "value": null },
            { "parameter": "suppressSubSchemaRetrieval", "value": true }
          ],
          "expected": "instance of JsonSchema returned (no exception)",
          "description": "Only validationContext, evaluationPath and schemaNode are provided; schemaLocation and parent are null while the suppress flag is true. The factory should still create a JsonSchema instance."
        },
        {
          "input": [
            { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
            { "parameter": "schemaLocation", "value": "mocked SchemaLocation instance" },
            { "parameter": "evaluationPath", "value": null },
            { "parameter": "schemaNode", "value": "mocked non‑null ObjectNode" },
            { "parameter": "parent", "value": "mocked parent JsonSchema instance" },
            { "parameter": "suppressSubSchemaRetrieval", "value": true }
          ],
          "expected": "instance of JsonSchema returned (no exception)",
          "description": "evaluationPath is null while all other required arguments are present and suppress flag is true. The method should handle the missing evaluationPath gracefully."
        },
        {
          "input": [
            { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
            { "parameter": "schemaLocation", "value": null },
            { "parameter": "evaluationPath", "value": null },
            { "parameter": "schemaNode", "value": "mocked non‑null ObjectNode" },
            { "parameter": "parent", "value": "mocked parent JsonSchema instance" },
            { "parameter": "suppressSubSchemaRetrieval", "value": true }
          ],
          "expected": "instance of JsonSchema returned (no exception)",
          "description": "Both schemaLocation and evaluationPath are null, parent is supplied, and suppress flag is true. The creation should still succeed."
        }
      ]
    },
    {
      "group": "Creation with various JsonNode implementations",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
            { "parameter": "schemaLocation", "value": "mocked SchemaLocation instance" },
            { "parameter": "evaluationPath", "value": "mocked JsonNodePath instance" },
            { "parameter": "schemaNode", "value": "mocked ArrayNode (empty array)" },
            { "parameter": "parent", "value": "mocked parent JsonSchema instance" },
            { "parameter": "suppressSubSchemaRetrieval", "value": false }
          ],
          "expected": "instance of JsonSchema returned (no exception)",
          "description": "The schemaNode is an ArrayNode instead of the usual ObjectNode. The factory method should accept any non‑null JsonNode."
        },
        {
          "input": [
            { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
            { "parameter": "schemaLocation", "value": "mocked SchemaLocation instance" },
            { "parameter": "evaluationPath", "value": "mocked JsonNodePath instance" },
            { "parameter": "schemaNode", "value": "mocked BooleanNode (true)" },
            { "parameter": "parent", "value": null },
            { "parameter": "suppressSubSchemaRetrieval", "value": false }
          ],
          "expected": "instance of JsonSchema returned (no exception)",
          "description": "The schemaNode is a BooleanNode. Even though a boolean schema is unusual, the method only requires a non‑null JsonNode."
        },
        {
          "input": [
            { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
            { "parameter": "schemaLocation", "value": "mocked SchemaLocation instance" },
            { "parameter": "evaluationPath", "value": "mocked JsonNodePath instance" },
            { "parameter": "schemaNode", "value": "mocked TextNode (\"example\")" },
            { "parameter": "parent", "value": "mocked parent JsonSchema instance" },
            { "parameter": "suppressSubSchemaRetrieval", "value": true }
          ],
          "expected": "instance of JsonSchema returned (no exception)",
          "description": "The schemaNode is a TextNode containing a string. The method should still construct a JsonSchema instance without error."
        }
      ]
    },
    {
      "group": "Creation with parent null and suppress flag variations",
      "cases": [
        {
          "input": [
            { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
            { "parameter": "schemaLocation", "value": "mocked SchemaLocation instance" },
            { "parameter": "evaluationPath", "value": "mocked JsonNodePath instance" },
            { "parameter": "schemaNode", "value": "mocked non‑null ObjectNode" },
            { "parameter": "parent", "value": null },
            { "parameter": "suppressSubSchemaRetrieval", "value": false }
          ],
          "expected": "instance of JsonSchema returned (no exception)",
          "description": "Parent schema is omitted (null) while all other arguments are present and suppress flag is false. The factory should still succeed."
        },
        {
          "input": [
            { "parameter": "validationContext", "value": "mocked ValidationContext instance" },
            { "parameter": "schemaLocation", "value": "mocked SchemaLocation instance" },
            { "parameter": "evaluationPath", "value": "mocked JsonNodePath instance" },
            { "parameter": "schemaNode", "value": "mocked non‑null ObjectNode" },
            { "parameter": "parent", "value": null },
            { "parameter": "suppressSubSchemaRetrieval", "value": true }
          ],
          "expected": "instance of JsonSchema returned (no exception)",
          "description": "Parent schema is null and suppressSubSchemaRetrieval is true. The method must handle a missing parent regardless of the flag."
        }
      ]
    }
  ]
}