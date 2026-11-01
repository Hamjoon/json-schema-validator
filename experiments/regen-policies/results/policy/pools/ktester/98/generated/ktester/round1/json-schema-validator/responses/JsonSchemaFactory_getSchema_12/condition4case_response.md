{
    "testCases": [
        {
            "group": "Valid inputs – normal flow",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/schema.json"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"
                        }
                    ],
                    "expected": "A non‑null JsonSchema instance whose SchemaLocation is \"http://example.com/schema.json\" and whose root node equals the supplied JsonNode.",
                    "description": "Typical usage – a well‑formed absolute URI and a valid JSON schema node. The method should create a JsonSchema via newJsonSchema and return it."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "file:///tmp/my-schema.yaml"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "{}"
                        }
                    ],
                    "expected": "A non‑null JsonSchema instance whose SchemaLocation is \"file:///tmp/my-schema.yaml\" and whose root node is an empty ObjectNode.",
                    "description": "URI points to a file scheme and the JSON node is an empty object. The method must still forward the values unchanged."
                }
            ]
        },
        {
            "group": "Null or missing parameters – exceptional paths",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": null
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "{\"type\":\"string\"}"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the URI argument is null the call to schemaUri.toString() throws NPE. The method should not swallow the exception."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/schema.json"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": null
                        }
                    ],
                    "expected": "A non‑null JsonSchema instance whose root node is null (the factory must accept a null JsonNode).",
                    "description": "The JSON node may be null – the factory forwards it to newJsonSchema which is allowed to handle a null schema node."
                }
            ]
        },
        {
            "group": "Malformed URI – error handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "ht!tp://[invalid]"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "{\"type\":\"integer\"}"
                        }
                    ],
                    "expected": "IllegalArgumentException or URISyntaxException wrapped in RuntimeException",
                    "description": "A syntactically invalid URI string causes SchemaLocation.of(...) to fail. The method should propagate the exception."
                }
            ]
        },
        {
            "group": "Custom SchemaValidatorsConfig – indirect branch coverage",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/schema.json"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "{\"type\":\"array\"}"
                        }
                    ],
                    "expected": "A JsonSchema whose internal SchemaValidatorsConfig reflects the default configuration returned by createSchemaValidatorsConfig().",
                    "description": "Even though getSchema does not expose the config, we verify that the factory uses the default config by inspecting a known behaviour (e.g., a custom format validator is not enabled)."
                }
            ]
        }
    ]
}