{
    "groups": [
        {
            "group": "walk(ExecutionContext, JsonNode, boolean, ExecutionContextCustomizer) - execution context immutability exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext (attributes map is unmodifiable Collections.unmodifiableMap)"
                        },
                        {
                            "parameter": "node",
                            "value": "ObjectNode {\"name\":\"Alice\"} (valid JSON object)"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "customizer that attempts to put attribute \"mode\"=\"test\" into the ExecutionContext"
                        }
                    ],
                    "expected": "UnsupportedOperationException propagated from the customizer when it tries to modify the immutable attribute map",
                    "description": "The ExecutionContext provided has an immutable attribute map. The customizer tries to mutate the context, which should cause an UnsupportedOperationException that must not be swallowed by walk."
                }
            ]
        },
        {
            "group": "walk(ExecutionContext, JsonNode, boolean, ExecutionContextCustomizer) - unresolved $ref exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext (valid, empty context)"
                        },
                        {
                            "parameter": "node",
                            "value": "ObjectNode {\"ref\":\"#/nonexistent\"} (JSON containing an invalid $ref)"
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
                    "expected": "JsonSchemaException (or RuntimeException) with message containing \"Unable to resolve reference\" indicating that $ref could not be resolved",
                    "description": "The JsonSchema instance is built from a schema that contains a $ref to a non‑existent definition. When walk processes the node, the reference resolution fails and a JsonSchemaException should be thrown."
                }
            ]
        },
        {
            "group": "walk(ExecutionContext, JsonNode, boolean, ExecutionContextCustomizer) - atRoot() illegal state",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext (instanceLocation field is null, causing atRoot() to throw IllegalStateException)"
                        },
                        {
                            "parameter": "node",
                            "value": "ObjectNode {\"type\":\"string\"} (valid JSON object)"
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
                    "expected": "IllegalStateException with message containing \"instanceLocation\" propagated from atRoot()",
                    "description": "If the ExecutionContext lacks a proper instanceLocation, the internal call to atRoot() throws IllegalStateException. walk must let this exception bubble up."
                }
            ]
        }
    ]
}