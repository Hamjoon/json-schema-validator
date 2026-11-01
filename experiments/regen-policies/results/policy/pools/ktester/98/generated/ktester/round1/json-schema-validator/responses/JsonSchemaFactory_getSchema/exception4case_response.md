{
    "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - malformed JSON syntax",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } "
                },
                {
                    "parameter": "config",
                    "value": "null"
                }
            ],
            "expected": "JsonProcessingException (or JsonSchemaException) due to invalid JSON syntax",
            "description": "A schema string that is syntactically invalid JSON (missing closing braces) should cause a parsing exception."
        }
    ]
}