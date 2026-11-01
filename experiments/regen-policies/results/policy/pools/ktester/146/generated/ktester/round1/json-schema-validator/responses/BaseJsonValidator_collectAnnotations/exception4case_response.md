{
    "test_cases": [
        {
            "group": "Null schemaNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContextMock { collectAnnotations = true, schemaNode = null, keyword = \"type\" }"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the validator's `schemaNode` field is null, the internal call to `schemaNode.has(\"type\")` (or similar) triggers a NullPointerException. The method should not swallow this error."
                }
            ]
        },
        {
            "group": "Null instance node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContextMock { collectAnnotations = true, instanceNode = null, keyword = \"type\" }"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If the `ExecutionContext` returns a null instance node (the JSON node being validated), any attempt to read its location or value inside `collectAnnotations` results in a NullPointerException."
                }
            ]
        },
        {
            "group": "Unmodifiable annotations list",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContextMock { collectAnnotations = true, annotations = Collections.unmodifiableList(new ArrayList<>()), keyword = \"type\" }"
                        }
                    ],
                    "expected": "UnsupportedOperationException",
                    "description": "When the context provides an immutable annotations collection, `putAnnotation` tries to add a new `JsonNodeAnnotation` and the underlying `add` call throws UnsupportedOperationException."
                }
            ]
        }
    ]
}