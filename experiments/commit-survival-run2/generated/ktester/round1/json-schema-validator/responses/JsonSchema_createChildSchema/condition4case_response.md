{
    "testCases": [
        {
            "group": "Normal behavior – valid inputs",
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
                    "expected": "A non‑null JsonSchema instance whose getParent() returns the original JsonSchema and whose getCurrentUri() matches the URI supplied to the ValidationContext. No exception is thrown.",
                    "description": "Verifies that createChildSchema forwards the supplied path and node to ValidationContext.newSchema and returns the newly created child schema."
                }
            ]
        },
        {
            "group": "Null schemaPath",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaPath",
                            "value": "null"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "ObjectNode representing {\"type\":\"integer\"}"
                        }
                    ],
                    "expected": "A non‑null JsonSchema child schema is returned. The underlying ValidationContext should receive a null path (allowed by the implementation). No exception is thrown.",
                    "description": "Ensures that a null schemaPath does not cause a NullPointerException and is correctly propagated to the ValidationContext."
                }
            ]
        },
        {
            "group": "Null schemaNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaPath",
                            "value": "\"#/properties/age\""
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "null"
                        }
                    ],
                    "expected": "A non‑null JsonSchema child schema is returned. ValidationContext receives a null JsonNode. No exception is thrown.",
                    "description": "Checks that a null JsonNode argument is accepted and passed through to the ValidationContext."
                }
            ]
        },
        {
            "group": "Both arguments null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaPath",
                            "value": "null"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "null"
                        }
                    ],
                    "expected": "A non‑null JsonSchema child schema is returned. ValidationContext receives both arguments as null. No exception is thrown.",
                    "description": "Validates that the method can handle both parameters being null simultaneously."
                }
            ]
        },
        {
            "group": "ValidationContext returns null (defensive test)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaPath",
                            "value": "\"#/properties/email\""
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "ObjectNode representing {\"format\":\"email\"}"
                        }
                    ],
                    "expected": "NullPointerException is thrown when the returned child schema is accessed, because getValidationContext().newSchema returned null. The test should assert that createChildSchema returns null.",
                    "description": "Simulates a mis‑behaving ValidationContext that returns null from newSchema to verify that the method does not swallow the null and that the caller sees the null result."
                }
            ]
        }
    ]
}