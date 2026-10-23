{
    "test_cases": [
        {
            "group": "Relative URI handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "schemas/schema.json"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" } } }"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A relative URI (no scheme) should be rejected because the factory expects an absolute URI that can be used as a schema identifier."
                }
            ]
        },
        {
            "group": "Unsupported scheme (data URI)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "data:application/json,{\"type\":\"object\"}"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"type\": \"object\" }"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Data‑URI scheme is not supported by JsonSchemaFactory; invoking getSchema with such a URI must throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Non‑object JsonNode (array)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/schema.json"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "[ { \"type\": \"object\" } ]"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The schema node must be a JSON object. Supplying a JSON array should cause the factory to reject the input with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Non‑object JsonNode (primitive)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/schema.json"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "\"string\""
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A primitive JSON value (string, number, boolean, etc.) cannot represent a schema. Passing such a JsonNode must trigger an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Invalid schema content",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/schema.json"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"type\": \"unknown\" }"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The schema contains an invalid value for the \"type\" keyword. The factory should validate schema keywords and throw an IllegalArgumentException for unsupported/unknown types."
                }
            ]
        }
    ]
}