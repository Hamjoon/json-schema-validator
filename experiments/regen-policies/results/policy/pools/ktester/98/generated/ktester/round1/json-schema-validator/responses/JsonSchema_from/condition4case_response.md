{
    "group": "JsonSchema.from - normal usage",
    "cases": [
        {
            "input": [
                { "parameter": "validationContext", "value": "new ValidationContext()" },
                { "parameter": "schemaLocation", "value": "new SchemaLocation(\"http://example.com/schema\")" },
                { "parameter": "evaluationPath", "value": "new JsonNodePath(\"$\")" },
                { "parameter": "schemaNode", "value": "JsonNodeFactory.instance.objectNode()" },
                { "parameter": "parent", "value": "null" },
                { "parameter": "suppressSubSchemaRetrieval", "value": "false" }
            ],
            "expected": "returns a non‑null JsonSchema instance whose getValidationContext() returns the supplied ValidationContext and isRecursiveAnchor() is false",
            "description": "Verify that the factory method creates a JsonSchema object when all required arguments are provided and the suppress flag is false."
        },
        {
            "input": [
                { "parameter": "validationContext", "value": "new ValidationContext()" },
                { "parameter": "schemaLocation", "value": "new SchemaLocation(\"file:/tmp/schema.json\")" },
                { "parameter": "evaluationPath", "value": "new JsonNodePath(\"#\")" },
                { "parameter": "schemaNode", "value": "JsonNodeFactory.instance.objectNode()" },
                { "parameter": "parent", "value": "new JsonSchema(...)" },
                { "parameter": "suppressSubSchemaRetrieval", "value": "true" }
            ],
            "expected": "returns a non‑null JsonSchema instance; subsequent call to getSubSchema(anyPath) must return null because sub‑schema retrieval is suppressed",
            "description": "When suppressSubSchemaRetrieval is true the created schema should not attempt to resolve sub‑schemas; this is verified by calling getSubSchema on a known fragment and expecting null."
        }
    ]
}