{
    "group": "JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig) - null ObjectMapper",
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
                },
                {
                    "parameter": "factoryInstance",
                    "value": "Anonymous subclass of JsonSchemaFactory where getJsonMapper() returns null"
                }
            ],
            "expected": "NullPointerException is thrown (not wrapped in JsonSchemaException)",
            "description": "The ObjectMapper returned by getJsonMapper() is null; invoking readTree on it causes a NullPointerException which propagates out of getSchema without being wrapped."
        }
    ]
}