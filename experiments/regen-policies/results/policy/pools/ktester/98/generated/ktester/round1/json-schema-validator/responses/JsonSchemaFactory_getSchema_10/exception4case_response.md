{
    "testCases": [
        {
            "group": "nullSchemaUriOnly",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "null"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The schemaUri argument is null while a non‑null configuration is supplied; the factory must reject the call and throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "unsupportedProtocol",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "ftp://example.com/schema.json"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException (or JsonSchemaException) indicating unsupported URI scheme",
                    "description": "A SchemaLocation using an unsupported protocol (ftp) is provided; JsonSchemaFactory should fail early with an appropriate exception."
                }
            ]
        },
        {
            "group": "malformedUri",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://[invalid]"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The URI string is syntactically malformed; the factory must detect the problem and throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "nonJsonContent",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "classpath:/schemas/not-json.txt"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonSchemaException (or IOException) caused by parsing failure",
                    "description": "The resource exists but contains plain‑text (non‑JSON/YAML) data; reading it as a schema should raise a parsing‑related exception."
                }
            ]
        },
        {
            "group": "invalidSchemaDefinition",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "classpath:/schemas/invalid-schema.json"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonSchemaException indicating schema validation error",
                    "description": "The file is valid JSON but does not conform to JSON‑Schema syntax (e.g., unknown keyword, wrong type); the factory must throw a JsonSchemaException during loading."
                }
            ]
        },
        {
            "group": "unresolvedReferenceDuringPreload",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "classpath:/schemas/with-unresolved-ref-schema.json"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonSchemaException (or IOException) caused by missing $ref target",
                    "description": "The schema contains a $ref that points to a non‑existent external schema; preload should detect the missing reference and raise an exception."
                }
            ]
        },
        {
            "group": "unsupportedMetaSchema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "classpath:/schemas/unsupported-meta-schema.json"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException (or JsonSchemaException) indicating unknown meta‑schema IRI",
                    "description": "The schema declares a $schema IRI that the factory does not recognise; loading must fail with an appropriate exception."
                }
            ]
        }
    ]
}