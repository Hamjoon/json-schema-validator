{
    "testCases": [
        {
            "group": "Valid inputs",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "JsonMapperFactory.getMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
                        }
                    ],
                    "expected": "instance of JsonSchema whose getSchemaLocation().toString() equals \"http://example.com/schema.json\"",
                    "description": "Happy‑path: a well‑formed URI and a simple JSON schema node should result in a non‑null JsonSchema with the correct location."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "JsonMapperFactory.getMapper().readTree(\"{\\\"type\\\":\\\"string\\\"}\")"
                        }
                    ],
                    "expected": "instance of JsonSchema whose getSchemaLocation().toString() equals \"\" (empty string)",
                    "description": "Edge case: an empty string URI is still accepted; the factory should create a JsonSchema with an empty location."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"file:/tmp/space%20in%20name.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "JsonMapperFactory.getMapper().readTree(\"{\\\"type\\\":\\\"array\\\",\\\"items\\\":{\\\"type\\\":\\\"number\\\"}}\")"
                        }
                    ],
                    "expected": "instance of JsonSchema whose getSchemaLocation().toString() contains \"space in name\" after decoding",
                    "description": "URI with encoded spaces – verify that the location is stored correctly after normalization."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"urn:example:schema\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "JsonMapperFactory.getMapper().readTree(\"{\\\"$schema\\\":\\\"http://json-schema.org/draft-07/schema#\\\",\\\"type\\\":\\\"null\\\"}\")"
                        }
                    ],
                    "expected": "instance of JsonSchema whose meta‑schema is Draft‑07 (as indicated by $schema) and location equals \"urn:example:schema\"",
                    "description": "Non‑hierarchical URI (URN) with a $schema keyword – ensures the factory forwards the node unchanged."
                }
            ]
        },
        {
            "group": "Invalid inputs – null arguments",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "null"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "JsonMapperFactory.getMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a null URI should cause the method to throw NPE because SchemaLocation.of(null) cannot be created."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a null JsonNode should result in NPE when newJsonSchema attempts to process the node."
                },
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
                    "description": "Both parameters null – the first null encountered (schemaUri) should trigger the exception."
                }
            ]
        },
        {
            "group": "Edge cases – malformed URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"ht!tp://[invalid]\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "JsonMapperFactory.getMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
                        }
                    ],
                    "expected": "URISyntaxException wrapped in IllegalArgumentException (or RuntimeException) when SchemaLocation.of throws",
                    "description": "A syntactically invalid URI should cause the factory to fail early, propagating the exception."
                }
            ]
        }
    ]
}