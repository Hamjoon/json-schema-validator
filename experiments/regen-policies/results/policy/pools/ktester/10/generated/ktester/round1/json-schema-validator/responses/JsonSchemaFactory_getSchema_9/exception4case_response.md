{
    "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode) - invalid SchemaLocation",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(\"http://[invalid-uri]\")"
                },
                {
                    "parameter": "jsonNode",
                    "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"string\")"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "The SchemaLocation contains a malformed URI. newJsonSchema should reject it and throw IllegalArgumentException, which getSchema must propagate."
        }
    ]
}