{
    "group": "walk(ExecutionContext, JsonNode, boolean, ExecutionContextCustomizer) - validation failure cases",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext (valid, empty context)"
                },
                {
                    "parameter": "node",
                    "value": "ObjectNode {\"age\": \"twenty\"} (string where schema expects integer)"
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
            "expected": "ValidationResult.failure() – contains a ValidationMessage with message key 'type' indicating expected type 'integer' but found 'string'",
            "description": "The schema for the root expects an integer property 'age'. Providing a string should cause walk to return a failed ValidationResult with the appropriate type‑mismatch message."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext (valid, empty context)"
                },
                {
                    "parameter": "node",
                    "value": "NullNode instance (JSON null literal)"
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
            "expected": "ValidationResult.failure() – contains a ValidationMessage with message key 'type' because the root schema does not allow null",
            "description": "When the root schema does not permit null values, walking a NullNode must produce a failed ValidationResult with a type‑error message."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext (valid, empty context)"
                },
                {
                    "parameter": "node",
                    "value": "BooleanNode true (JSON boolean)"
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
            "expected": "ValidationResult.failure() – contains a ValidationMessage with message key 'type' indicating expected type 'object' but found 'boolean'",
            "description": "The root schema expects an object; supplying a boolean should trigger a type‑validation failure."
        }
    ]
}