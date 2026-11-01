{
    "test_cases": [
        {
            "group": "atRoot null handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock(ExecutionContext.class)"
                        },
                        {
                            "parameter": "node",
                            "value": "ObjectNodeFactory.createObjectNode().put(\"field\", \"value\")"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "A concrete subclass of BaseJsonValidator overrides atRoot() to return null and the four‑argument validate method dereferences the supplied path (e.g., calling path.toString()). When BaseJsonValidator.validate forwards the null path, the overridden method throws a NullPointerException which must propagate unchanged."
                }
            ]
        },
        {
            "group": "atRoot exception propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock(ExecutionContext.class)"
                        },
                        {
                            "parameter": "node",
                            "value": "ObjectNodeFactory.createObjectNode().put(\"foo\", 123)"
                        }
                    ],
                    "expected": "IllegalStateException with message \"atRoot failed\"",
                    "description": "A concrete subclass of BaseJsonValidator overrides atRoot() to throw an IllegalStateException. The BaseJsonValidator.validate method should not catch this exception; it must propagate the IllegalStateException to the caller."
                }
            ]
        }
    ]
}