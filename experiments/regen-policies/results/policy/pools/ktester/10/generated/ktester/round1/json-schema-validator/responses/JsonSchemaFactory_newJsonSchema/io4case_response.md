{
    "testCases": [
        {
            "group": "newJsonSchema - default config creation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "mock(SchemaLocation.class)"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "mock(JsonNode.class)"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonSchema instance returned; internally JsonSchemaFactory.createSchemaValidatorsConfig() is invoked to supply a non‑null SchemaValidatorsConfig to createValidationContext; initializeValidators() is called once.",
                    "description": "When config is null the factory must create a default SchemaValidatorsConfig. The test verifies that a non‑null config is supplied to createValidationContext and that the method still returns a valid JsonSchema."
                }
            ]
        },
        {
            "group": "newJsonSchema - custom config propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "mock(SchemaLocation.class)"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "mock(JsonNode.class)"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchema instance returned; the same SchemaValidatorsConfig instance supplied is passed unchanged to createValidationContext and subsequently to doCreate.",
                    "description": "Ensures that when a non‑null config is provided it is not replaced by a default config and is used throughout schema creation."
                }
            ]
        },
        {
            "group": "newJsonSchema - null schemaUri handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "null"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "mock(JsonNode.class)"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonSchema instance returned; getSchemaLocation receives a null schemaRetrievalUri and must resolve a location based on the schemaNode content (e.g., using an empty base URI). No NullPointerException is thrown.",
                    "description": "Verifies that the method can handle a null SchemaLocation argument, delegating to getSchemaLocation which should treat the missing URI as an empty base and still produce a valid JsonSchema."
                }
            ]
        },
        {
            "group": "newJsonSchema - null schemaNode throws NPE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "mock(SchemaLocation.class)"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "null"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException thrown from createValidationContext or doCreate before any JsonSchema is returned.",
                    "description": "The method does not guard against a null schemaNode; passing null should cause a NullPointerException when the node is accessed inside createValidationContext or doCreate."
                }
            ]
        },
        {
            "group": "newJsonSchema - doCreate returns null leads to NPE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "mock(SchemaLocation.class)"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "mock(JsonNode.class)"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException thrown when jsonSchema.initializeValidators() is invoked on a null reference; the exception propagates out of newJsonSchema.",
                    "description": "Mocks doCreate to return null. The subsequent call to jsonSchema.initializeValidators() dereferences null, causing a NPE that is not caught by the method. The test confirms that such a situation results in an exception rather than a silent failure."
                }
            ]
        }
    ]
}