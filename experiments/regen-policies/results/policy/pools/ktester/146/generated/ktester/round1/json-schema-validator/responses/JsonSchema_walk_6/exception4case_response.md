{
    "groups": [
        {
            "group": "walk_InvalidRef_ShouldThrowJsonSchemaException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created via schemaWithInvalidRef.createExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": "Valid JSON node (e.g., {\"foo\":\"bar\"})"
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
                    "expected": "JsonSchemaException (or IllegalArgumentException) indicating that $ref '#/nonExisting' cannot be resolved",
                    "description": "The schema contains a $ref that points to a fragment that does not exist. During walk the resolver must throw a JsonSchemaException (or IllegalArgumentException)."
                }
            ]
        },
        {
            "group": "walk_RecursiveSchema_ExceedsDepth_ShouldThrowStackOverflowError",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created via recursiveSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": "JSON that matches the recursive schema (e.g., {\"next\":{}})"
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
                    "expected": "StackOverflowError (or IllegalStateException if the implementation caps recursion depth)",
                    "description": "The schema contains an un‑bounded recursive $ref without a $recursiveAnchor. Walking such a schema should eventually hit the recursion limit and throw a StackOverflowError (or a bounded‑depth IllegalStateException)."
                }
            ]
        },
        {
            "group": "walk_CustomizerThrowsNullPointerException_ShouldPropagate",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created via schema.createExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": "Valid JSON node according to the schema"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "Consumer that throws NullPointerException(\"customizer NPE\")"
                        }
                    ],
                    "expected": "NullPointerException with message \"customizer NPE\"",
                    "description": "If the consumer supplied as executionCustomizer throws a NullPointerException, walk must let that exception propagate unchanged."
                }
            ]
        },
        {
            "group": "walk_CustomizerModifiesImmutableContext_ShouldThrowUnsupportedOperationException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created via schema.createExecutionContext() whose custom data map is made immutable (e.g., Collections.unmodifiableMap(...))"
                        },
                        {
                            "parameter": "node",
                            "value": "Valid JSON node according to the schema"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "Consumer that attempts executionContext.putCustomData(\"key\", \"value\")"
                        }
                    ],
                    "expected": "UnsupportedOperationException",
                    "description": "When the customizer tries to modify an immutable ExecutionContext, the underlying map throws UnsupportedOperationException, which must propagate out of walk."
                }
            ]
        }
    ]
}