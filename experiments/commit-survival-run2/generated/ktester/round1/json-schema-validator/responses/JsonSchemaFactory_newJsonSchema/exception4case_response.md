{
    "test_cases": [
        {
            "group": "Invalid schema node – numeric literal",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/numeric.json\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "new com.fasterxml.jackson.databind.node.IntNode(42)"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The factory expects the root schema node to be an object, array, or boolean. Supplying a numeric literal should cause an IllegalArgumentException during schema creation."
                }
            ]
        },
        {
            "group": "Invalid schema node – MissingNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/missing.json\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "com.fasterxml.jackson.databind.node.MissingNode.getInstance()"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A MissingNode represents the absence of a JSON value. Passing it as the schema root should trigger an IllegalArgumentException because the factory cannot build a JsonSchema from a non‑existent node."
                }
            ]
        },
        {
            "group": "Unsupported URI scheme – should raise exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"unknown://example.com/schema.json\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The factory’s internal URITranslator only supports http, https, file and classpath schemes. Supplying an unknown scheme should cause an IllegalArgumentException during URI translation."
                }
            ]
        },
        {
            "group": "Null configuration – internal NPE (if not tolerated)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/null-config.json\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"string\\\"}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If the implementation of ValidationContext.setConfig does not accept a null argument, passing null should result in a NullPointerException. This test captures that potential failure mode."
                }
            ]
        }
    ]
}