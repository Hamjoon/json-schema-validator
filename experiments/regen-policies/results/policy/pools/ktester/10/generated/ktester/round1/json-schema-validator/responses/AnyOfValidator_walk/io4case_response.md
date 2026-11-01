{
    "testCases": [
        {
            "group": "shouldValidateSchema true – validate returns empty set",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "mockExecutionContext" },
                        { "parameter": "node", "value": "mockNode" },
                        { "parameter": "rootNode", "value": "mockRootNode" },
                        { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
                        { "parameter": "shouldValidateSchema", "value": true }
                    ],
                    "expected": "empty LinkedHashSet (the Set returned by validate is empty and is returned unchanged)",
                    "description": "When shouldValidateSchema is true and the validator’s validate method returns an empty Set, walk must forward that empty Set without further processing."
                }
            ]
        },
        {
            "group": "shouldValidateSchema true – validate returns null (defensive handling)",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "mockExecutionContext" },
                        { "parameter": "node", "value": "mockNode" },
                        { "parameter": "rootNode", "value": "mockRootNode" },
                        { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
                        { "parameter": "shouldValidateSchema", "value": true }
                    ],
                    "expected": "empty LinkedHashSet (null from validate is treated as empty)",
                    "description": "If validate unexpectedly returns null, walk should not throw NPE but instead return an empty Set, matching the contract of returning a Set of ValidationMessage."
                }
            ]
        },
        {
            "group": "shouldValidateSchema false – schemas walk return non‑empty sets (ignored)",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "mockExecutionContext" },
                        { "parameter": "node", "value": "mockNode" },
                        { "parameter": "rootNode", "value": "mockRootNode" },
                        { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
                        { "parameter": "shouldValidateSchema", "value": false }
                    ],
                    "expected": "empty LinkedHashSet (walk returns empty regardless of inner schema results)",
                    "description": "When shouldValidateSchema is false, AnyOfValidator must ignore the Sets returned by each inner schema’s walk call and always return an empty LinkedHashSet."
                }
            ]
        },
        {
            "group": "shouldValidateSchema false – schema.walk throws RuntimeException",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "mockExecutionContext" },
                        { "parameter": "node", "value": "mockNode" },
                        { "parameter": "rootNode", "value": "mockRootNode" },
                        { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
                        { "parameter": "shouldValidateSchema", "value": false }
                    ],
                    "expected": "throws RuntimeException (exception propagates)",
                    "description": "If any inner schema’s walk method throws a RuntimeException while shouldValidateSchema is false, AnyOfValidator must not catch it; the exception should propagate to the caller."
                }
            ]
        },
        {
            "group": "shouldValidateSchema false – duplicate schemas in list",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "mockExecutionContext" },
                        { "parameter": "node", "value": "mockNode" },
                        { "parameter": "rootNode", "value": "mockRootNode" },
                        { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
                        { "parameter": "shouldValidateSchema", "value": false }
                    ],
                    "expected": "empty LinkedHashSet and each duplicate schema.walk called once per occurrence",
                    "description": "When the internal schemas list contains the same JsonSchema instance multiple times, walk must invoke schema.walk for each occurrence (i.e., twice for two duplicates) and still return an empty Set."
                }
            ]
        },
        {
            "group": "shouldValidateSchema false – null node parameter",
            "cases": [
                {
                    "input": [
                        { "parameter": "executionContext", "value": "mockExecutionContext" },
                        { "parameter": "node", "value": null },
                        { "parameter": "rootNode", "value": "mockRootNode" },
                        { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
                        { "parameter": "shouldValidateSchema", "value": false }
                    ],
                    "expected": "throws NullPointerException",
                    "description": "Passing a null JsonNode for the node argument should cause a NullPointerException when the method forwards the call to the inner schemas (or validate). The test verifies defensive null handling."
                }
            ]
        }
    ]
}