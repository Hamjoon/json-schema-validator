{
    "testCases": [
        {
            "group": "ObtainSubSchemaNode Throws NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "mockValidationContext"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When suppressSubSchemaRetrieval = false, the private static obtainSubSchemaNode is mocked to throw a NullPointerException (simulating a null schemaNode). The exception must propagate out of fetchSubSchemaNode."
                }
            ]
        },
        {
            "group": "ObtainSubSchemaNode Throws IllegalArgumentException (Invalid SchemaNode)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "mockValidationContext"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "With suppressSubSchemaRetrieval = false, obtainSubSchemaNode is mocked to throw IllegalArgumentException because the supplied schemaNode is not a valid JSON Schema object. fetchSubSchemaNode should not catch it and must let it bubble up."
                }
            ]
        },
        {
            "group": "ObtainSubSchemaNode Throws IllegalArgumentException (Null ValidationContext)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When suppressSubSchemaRetrieval = false and a null ValidationContext is passed, obtainSubSchemaNode is mocked to throw IllegalArgumentException indicating that the context is required. The method should propagate this exception."
                }
            ]
        },
        {
            "group": "ObtainSubSchemaNode Throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "mockValidationContext"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "Simulate an unexpected runtime failure inside obtainSubSchemaNode (e.g., IO error while loading a referenced schema). The RuntimeException must be propagated by fetchSubSchemaNode."
                }
            ]
        },
        {
            "group": "Suppress SubSchema Retrieval Prevents Exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "mockValidationContext"
                        }
                    ],
                    "expected": "null",
                    "description": "Even if obtainSubSchemaNode is configured to throw an exception, setting suppressSubSchemaRetrieval = true short‑circuits the call, so fetchSubSchemaNode returns null and no exception is thrown."
                }
            ]
        }
    ]
}