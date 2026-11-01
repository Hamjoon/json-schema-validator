{
    "testCases": [
        {
            "group": "testGetSchemaWithNonObjectNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "\"just a string\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalArgumentException) indicating that the root schema node must be an object",
                    "description": "The supplied JsonNode is a JSON string literal rather than an object. The factory should reject it because a JSON‑Schema document must be a JSON object."
                }
            ]
        },
        {
            "group": "testGetSchemaWithUnresolvableRef",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"$ref\": \"http://nonexistent.example.com/schema.json\" }"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException indicating that the referenced schema could not be loaded or resolved",
                    "description": "The schema contains a $ref that points to a URI which cannot be fetched. The factory must raise an exception rather than returning a partially built schema."
                }
            ]
        },
        {
            "group": "testGetSchemaWithInvalidIdUri",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"$id\": \"ht!tp://[invalid]\", \"type\": \"object\" }"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException (or JsonSchemaException) caused by malformed $id URI",
                    "description": "The $id keyword contains an invalid URI. The factory should detect the malformed identifier and throw an appropriate exception."
                }
            ]
        },
        {
            "group": "testGetSchemaWithCircularReference",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"$id\": \"http://example.com/root\", \"$ref\": \"http://example.com/root\" }"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException indicating circular $ref detection",
                    "description": "The schema refers to itself directly, creating a circular reference. The factory must detect this situation and abort with a clear exception rather than entering infinite recursion."
                }
            ]
        }
    ]
}