{
    "test_cases": [
        {
            "group": "Null fragment argument – NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException (thrown when fragment.getNameCount() is invoked)",
                    "description": "The method is called with a null JsonNodePath. Accessing fragment.getNameCount() should cause a NullPointerException, verifying that the method does not silently accept null pointers."
                }
            ]
        },
        {
            "group": "Null ValidationContext – NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath with a single string segment ['title']"
                        },
                        {
                            "parameter": "validationContext",
                            "value": "null (parent schema's validationContext is not set)"
                        }
                    ],
                    "expected": "NullPointerException (thrown when parent.getValidationContext().newSchema(...) is invoked)",
                    "description": "The root schema and its ancestors exist, but the ValidationContext of the current parent is null. When the method attempts to create a new sub‑schema via newSchema, a NullPointerException should be raised."
                }
            ]
        },
        {
            "group": "Null document root – NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath with a single string segment ['title']"
                        },
                        {
                            "parameter": "documentRoot",
                            "value": "null (findSchemaResourceRoot() returns null)"
                        }
                    ],
                    "expected": "NullPointerException (thrown when document.findSchemaResourceRoot() is accessed)",
                    "description": "Simulate a situation where findSchemaResourceRoot() returns null (e.g., malformed schema). The subsequent call to document.findSchemaResourceRoot().fetchSubSchemaNode(...) will cause a NullPointerException."
                }
            ]
        },
        {
            "group": "Unsupported segment type – IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath with a single custom object segment [{\"type\":\"Unsupported\"}]"
                        }
                    ],
                    "expected": "IllegalArgumentException (thrown by parent.getNode(segment) for an unsupported segment type)",
                    "description": "The segment object is neither a String nor a Number. The implementation of getNode(Object) is expected to reject such types and throw IllegalArgumentException, which should propagate out of getSubSchema."
                }
            ]
        },
        {
            "group": "fetchSubSchemaNode throws JsonProcessingException – propagated exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "fragment",
                            "value": "JsonNodePath with a single string segment ['missing']"
                        },
                        {
                            "parameter": "fetchSubSchemaNodeBehavior",
                            "value": "throws JsonProcessingException"
                        }
                    ],
                    "expected": "JsonProcessingException (propagated from fetchSubSchemaNode)",
                    "description": "When the fragment cannot be resolved in the current document, the method calls document.findSchemaResourceRoot().fetchSubSchemaNode(...). If that method throws a JsonProcessingException, getSubSchema should not catch it, allowing the exception to bubble up."
                }
            ]
        }
    ]
}