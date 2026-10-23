{
    "test_cases": [
        {
            "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - Null Schema String (IllegalArgumentException)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "null"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown",
                    "description": "When the supplied schema string is null, ObjectMapper.readTree(null) throws IllegalArgumentException. The method does not catch this runtime exception, so it propagates unchanged."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - Null JsonMapper (NullPointerException)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "{\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "NullPointerException thrown",
                    "description": "If the JsonSchemaFactory instance is constructed (or modified via reflection) with a null ObjectMapper, the call to jsonMapper.readTree(schema) results in a NullPointerException, which is not caught by the method."
                }
            ]
        }
    ]
}