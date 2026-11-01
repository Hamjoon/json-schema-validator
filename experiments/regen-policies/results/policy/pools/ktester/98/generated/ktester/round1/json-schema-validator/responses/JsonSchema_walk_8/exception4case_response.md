{
    "groups": [
        {
            "group": "Unsupported InputFormat value",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"name\":\"Test\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.XML"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": null
                        }
                    ],
                    "expected": "throws IllegalArgumentException with message containing 'Unsupported input format'",
                    "description": "The walk method should reject an InputFormat that it does not support (e.g., XML) and raise an IllegalArgumentException before any deserialization occurs."
                }
            ]
        },
        {
            "group": "createExecutionContext returns null leading to NPE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"valid\":\"json\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": null
                        }
                    ],
                    "expected": "throws NullPointerException when walk attempts to use a null ExecutionContext",
                    "description": "If a subclass of JsonSchema overrides createExecutionContext() to return null, the subsequent walk call should fail with a NullPointerException because the method expects a non‑null ExecutionContext."
                }
            ]
        },
        {
            "group": "Deserialization returns null node (input string 'null')",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "null"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": null
                        }
                    ],
                    "expected": "throws ValidationException (or IllegalStateException) indicating that the root node is null and cannot be walked",
                    "description": "When the JSON payload is the literal 'null', deserialize() returns a JsonNode representing JSON null. The walk method is expected to treat this as an invalid root for schema validation and raise an exception rather than returning a successful ValidationResult."
                }
            ]
        }
    ]
}