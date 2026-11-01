{
    "test_cases": [
        {
            "group": "BaseJsonValidator.fetchSubSchemaNode.exceptions",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "Mocked ValidationContext (no special behavior required)"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When suppressSubSchemaRetrieval = false and the validator's schemaNode is an ArrayNode (JSON array), the private obtainSubSchemaNode method is expected to reject non‑object schema definitions and throw IllegalArgumentException. fetchSubSchemaNode must propagate this exception."
                },
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "Mocked ValidationContext (no special behavior required)"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When suppressSubSchemaRetrieval = false and the validator's schemaNode is an ObjectNode that contains a $ref together with other schema keywords (e.g., \"type\"), obtainSubSchemaNode validates the rule that $ref must be the sole keyword and throws IllegalArgumentException. The test verifies that fetchSubSchemaNode does not swallow the exception."
                },
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "Mocked ValidationContext that throws RuntimeException on any method call"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "If the supplied ValidationContext is a spy/mock whose method (e.g., getSchemaMap) throws a RuntimeException, obtainSubSchemaNode will encounter this exception while trying to resolve references. fetchSubSchemaNode should let the RuntimeException bubble up."
                },
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "Mocked ValidationContext (no special behavior required)"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "When suppressSubSchemaRetrieval = false and the underlying schemaNode is syntactically valid but semantically invalid (e.g., contains an unknown keyword), obtainSubSchemaNode throws a JsonSchemaException (a runtime exception specific to the library). The test ensures fetchSubSchemaNode propagates this custom exception."
                }
            ]
        }
    ]
}