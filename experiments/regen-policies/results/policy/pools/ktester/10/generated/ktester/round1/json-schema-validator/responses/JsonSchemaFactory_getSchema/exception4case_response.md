{
    "groups": [
        {
            "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - unknown meta‑schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "\"{\\\"$schema\\\":\\\"http://unknown/meta/schema\\\",\\\"type\\\":\\\"object\\\"}\""
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonSchemaException is thrown (unknown meta‑schema id cannot be resolved)",
                    "description": "The JSON schema contains a $schema attribute that points to a meta‑schema identifier which the factory cannot load. newJsonSchema fails and propagates a JsonSchemaException that is not wrapped by the IOException catch block."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - literal null JSON value",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "\"null\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown (schemaNode is NullNode and cannot be used to build a JsonSchema)",
                    "description": "The supplied string is the JSON literal null. ObjectMapper.readTree returns a NullNode; when newJsonSchema is called it validates the node type and throws IllegalArgumentException, which is not caught by the method."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - getJsonMapper returns null (NPE)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "\"{}\""
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException is thrown (ObjectMapper is null, causing readTree to be invoked on a null reference)",
                    "description": "A test subclass of JsonSchemaFactory overrides protected getJsonMapper() to return null. When getSchema is invoked the method attempts to call readTree on a null ObjectMapper, resulting in a NullPointerException that propagates out of the method."
                }
            ]
        }
    ]
}