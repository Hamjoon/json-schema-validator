{
    "testCases": [
        {
            "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - Null Schema String",
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
                    "expected": "JsonSchemaException thrown",
                    "description": "The schema argument is null. ObjectMapper.readTree(null) throws an exception which should be wrapped in a JsonSchemaException by the method."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - Empty Schema String",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "\"\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException thrown",
                    "description": "An empty JSON string cannot be parsed; the method must catch the IOException and re‑throw it as a JsonSchemaException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - Malformed JSON",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "\"{type:object,}\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException thrown",
                    "description": "The supplied string is not valid JSON (missing quotes around field names). The method should propagate the parsing error as a JsonSchemaException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - Null Config",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "Non‑null JsonSchema instance",
                    "description": "When the configuration argument is null the method should still parse the JSON and create a JsonSchema using a default (null) config."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - Complex Valid Schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "{\n  \"type\": \"object\",\n  \"properties\": {\n    \"id\": {\"type\": \"integer\"},\n    \"name\": {\"type\": \"string\"},\n    \"tags\": {\n      \"type\": \"array\",\n      \"items\": {\"type\": \"string\"}\n    },\n    \"address\": {\n      \"type\": \"object\",\n      \"properties\": {\n        \"street\": {\"type\": \"string\"},\n        \"zip\": {\"type\": \"string\"}\n      },\n      \"required\": [\"street\"]\n    }\n  },\n  \"required\": [\"id\", \"name\"]\n}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "Non‑null JsonSchema instance",
                    "description": "A well‑formed, nested schema with arrays, objects and required fields should be parsed successfully and result in a JsonSchema object."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - Schema With Internal $ref",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "{\n  \"$id\": \"http://example.com/root.json\",\n  \"definitions\": {\n    \"positiveInteger\": {\"type\": \"integer\", \"minimum\": 0}\n  },\n  \"type\": \"object\",\n  \"properties\": {\n    \"count\": {\"$ref\": \"#\\/definitions\\/positiveInteger\"}\n  }\n}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "Non‑null JsonSchema instance",
                    "description": "The schema contains an internal $ref. The method only parses the JSON; resolution of the reference occurs later, so parsing must succeed and return a JsonSchema."
                }
            ]
        }
    ]
}