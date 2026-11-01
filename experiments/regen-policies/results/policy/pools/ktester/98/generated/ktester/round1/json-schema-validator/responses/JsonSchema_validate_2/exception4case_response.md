{
    "test_cases": [
        {
            "group": "Invalid $ref encoding triggers UnsupportedEncodingException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "{\"ref\":\"#%ZZ\"}"
                        }
                    ],
                    "expected": "UnsupportedEncodingException",
                    "description": "The schema contains a $ref with an invalid percent‑encoding (\"#%ZZ\"). During validation the implementation attempts to URL‑decode the fragment, which throws UnsupportedEncodingException. The exception should propagate (or be wrapped in a RuntimeException) when validate(JsonNode) is invoked."
                }
            ]
        },
        {
            "group": "MissingNode as root triggers IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "MissingNode"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Jackson's MissingNode represents a non‑existent node. Passing it as the root to validate(JsonNode) should cause the validator to reject the input and throw IllegalArgumentException because a concrete JSON value is required."
                }
            ]
        },
        {
            "group": "Circular $ref leads to StackOverflowError",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "{\"self\":\"$ref\"}"
                        }
                    ],
                    "expected": "StackOverflowError",
                    "description": "The schema defines a $ref that points to itself, creating an infinite recursion during schema resolution. Invoking validate(JsonNode) should eventually cause a StackOverflowError."
                }
            ]
        },
        {
            "group": "Root node of unexpected type (binary) triggers IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "BinaryNode"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When the root JsonNode is a BinaryNode (e.g., created via JsonNodeFactory.binaryNode()), the validator does not know how to process binary data for most schemas and should throw IllegalArgumentException."
                }
            ]
        }
    ]
}