{
    "testCases": [
        {
            "group": "Valid inputs – absolute URI with JSON node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"http://example.com/schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"string\")"
                        }
                    ],
                    "expected": "returns a non‑null JsonSchema instance whose meta‑schema matches the default version",
                    "description": "When a proper absolute SchemaLocation and a valid JsonNode are supplied, getSchema should create a JsonSchema without throwing and the returned object should be usable for validation."
                }
            ]
        },
        {
            "group": "Valid inputs – relative URI with JSON node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"/schemas/local-schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"integer\")"
                        }
                    ],
                    "expected": "returns a non‑null JsonSchema instance",
                    "description": "Relative URIs are resolved by the factory; the method should still return a JsonSchema instance."
                }
            ]
        },
        {
            "group": "Null schemaUri – should raise NPE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "null"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"boolean\")"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The method requires a non‑null SchemaLocation; passing null must result in a NullPointerException."
                }
            ]
        },
        {
            "group": "Null jsonNode – schema loaded from location",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"classpath:/schemas/valid-schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "null"
                        }
                    ],
                    "expected": "returns a non‑null JsonSchema instance loaded from the supplied location",
                    "description": "When jsonNode is null the factory should read the schema from the provided SchemaLocation and still return a JsonSchema."
                }
            ]
        },
        {
            "group": "Both parameters null – should raise NPE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "null"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Both arguments being null is illegal; the method must throw a NullPointerException before any processing."
                }
            ]
        },
        {
            "group": "Invalid URI format – malformed schemaUri",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"ht!tp://[invalid]\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"null\")"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A malformed URI should cause the factory to reject the SchemaLocation and throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Empty JSON node – schema without content",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"http://example.com/empty.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "JsonNodeFactory.instance.objectNode()"
                        }
                    ],
                    "expected": "returns a JsonSchema instance whose root node is empty (no validation rules)",
                    "description": "Even an empty JsonNode is a valid input; the method should still construct a JsonSchema object."
                }
            ]
        }
    ]
}