{
    "test_cases": [
        {
            "group": "Null ValidationContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaPath",
                            "value": "\"#/properties/name\""
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "ObjectNode representing {\"type\":\"string\"}"
                        }
                    ],
                    "expected": "NullPointerException thrown because getValidationContext() returns null and the method attempts to invoke newSchema on a null reference.",
                    "description": "Simulates a mis‑configured JsonSchema where the underlying ValidationContext is null. The method should not swallow the NPE; it must propagate it."
                }
            ]
        },
        {
            "group": "Invalid schemaPath causing URISyntaxException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaPath",
                            "value": "\"ht!tp://[invalid]\""
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "ObjectNode representing {\"type\":\"string\"}"
                        }
                    ],
                    "expected": "IllegalArgumentException (or a subclass) thrown by ValidationContext.newSchema when it attempts to treat the malformed schemaPath as a URI. The exception propagates out of createChildSchema.",
                    "description": "Provides a schemaPath that is not a valid URI. The mocked ValidationContext is configured to throw an IllegalArgumentException wrapping a URISyntaxException, verifying that createChildSchema does not catch or transform the exception."
                }
            ]
        },
        {
            "group": "ValidationContext throws IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaPath",
                            "value": "\"#/properties/invalid\""
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "ObjectNode representing {\"type\":\"unknown\"}"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown by ValidationContext.newSchema because the supplied schemaNode contains an unsupported \"type\" value. The same exception propagates unchanged.",
                    "description": "Mocks ValidationContext.newSchema to throw IllegalArgumentException for an unsupported schema type, ensuring the method does not swallow the exception."
                }
            ]
        },
        {
            "group": "ValidationContext throws IllegalStateException for malformed schemaNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaPath",
                            "value": "\"#/properties/missingRequired\""
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "ObjectNode representing {\"properties\":{\"name\":{\"type\":\"string\"}}}"
                        }
                    ],
                    "expected": "IllegalStateException thrown by ValidationContext.newSchema because the schemaNode lacks a required \"type\" field for a property definition. The exception propagates out of createChildSchema.",
                    "description": "Simulates a scenario where the schemaNode is syntactically valid JSON but semantically incomplete, causing ValidationContext to raise an IllegalStateException. The test confirms that createChildSchema does not intercept this exception."
                }
            ]
        }
    ]
}