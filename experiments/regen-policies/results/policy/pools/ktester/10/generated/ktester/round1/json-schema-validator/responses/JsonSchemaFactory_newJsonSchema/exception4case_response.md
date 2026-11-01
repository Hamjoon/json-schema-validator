Invalid control character at: line 33 column 65 (char 1460)
{
    "test_cases": [
        {
            "group": "newJsonSchema - createValidationContext throws",
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
                    "expected": "RuntimeException propagated; newJsonSchema does not catch exceptions from createValidationContext, so the method aborts with the same exception.",
                    "description": "A subclass or spy of JsonSchemaFactory overrides createValidationContext to throw a RuntimeException. The test verifies that the exception bubbles out of newJsonSchema and no JsonSchema is returned."
                }
            ]
        },
        {
            "group": "newJsonSchema - getSchemaLocation throws",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "mock(SchemaLocation.class) // configured to throw when used"
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
                    "expected": "IllegalArgumentException (or IOException) propagated; the exception from getSchemaLocation is not caught inside newJsonSchema.",
                    "description": "A spy of JsonSchemaFactory overrides getSchemaLocation to throw an IllegalArgumentException (e.g., due to malformed URI). The test confirms that newJsonSchema does not handle this exception and it propagates to the caller."
                }
            ]
        },
        {
            "group": "newJsonSchema - JsonNodePath construction throws NPE",
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
                            "value": "new SchemaValidatorsConfig() // with pathType set to null"
                        }
                    ],
                    "expected": "NullPointerException propagated; JsonNodePath constructor receives a null path type and throws NPE before doCreate is invoked.",
                    "description": "Configure the SchemaValidatorsConfig (or mock ValidationContext) so that validationContext.getConfig().getPathType() returns null. The resulting NullPointerException from new JsonNodePath should propagate out of newJsonSchema."
                }
            ]
        },
        {
            "group": "newJsonSchema - doCreate throws",
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
                    "expected": "IllegalStateException propagated; doCreate throws and the exception is not caught by newJsonSchema.",
                    "description": "A spy of JsonSchemaFactory overrides doCreate to throw an IllegalStateException (simulating a failure while constructing the JsonSchema). The test verifies that the exception propagates out of newJsonSchema."
                }
            ]
        }
    ]
}