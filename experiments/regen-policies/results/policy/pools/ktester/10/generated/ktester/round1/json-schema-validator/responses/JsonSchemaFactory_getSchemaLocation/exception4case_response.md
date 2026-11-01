{
    "group": "JsonSchemaFactory.getSchemaLocation",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaRetrievalUri",
                    "value": "null"
                },
                {
                    "parameter": "schemaNode",
                    "value": "any JsonNode (mocked)"
                },
                {
                    "parameter": "validationContext",
                    "value": "mock ValidationContext where resolveSchemaId(schemaNode) throws IllegalStateException"
                }
            ],
            "expected": "IllegalStateException",
            "description": "When the ValidationContext throws an exception while resolving the schema ID, the exception should propagate out of getSchemaLocation."
        },
        {
            "input": [
                {
                    "parameter": "schemaRetrievalUri",
                    "value": "null"
                },
                {
                    "parameter": "schemaNode",
                    "value": "any JsonNode (mocked)"
                },
                {
                    "parameter": "validationContext",
                    "value": "mock ValidationContext where resolveSchemaId(schemaNode) returns \"http://[invalid]\""
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "A malformed URI string returned from ValidationContext should cause SchemaLocation.of to throw IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "schemaRetrievalUri",
                    "value": "SchemaLocation.of(\"ht!tp://bad\")"
                },
                {
                    "parameter": "schemaNode",
                    "value": "any JsonNode (mocked)"
                },
                {
                    "parameter": "validationContext",
                    "value": "mock ValidationContext where resolveSchemaId(schemaNode) returns null"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "When ValidationContext returns null and the fallback schemaRetrievalUri contains an invalid URI string, SchemaLocation.of should throw IllegalArgumentException."
        }
    ]
}