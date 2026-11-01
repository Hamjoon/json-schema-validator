{
    "tests": [
        {
            "group": "Propagation of node reference",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock(ExecutionContext.class)"
                        },
                        {
                            "parameter": "node",
                            "value": "ObjectNodeFactory.createObjectNode().put(\"key\", \"value\")"
                        }
                    ],
                    "expected": "The overloaded validate(executionContext, node, originalNode, atRoot()) receives the same instance for the third argument (originalNode) as the node supplied to the single‑argument validate method.",
                    "description": "Verifies that BaseJsonValidator.validate forwards the original JsonNode unchanged, i.e., the reference identity of the third parameter equals the original node."
                }
            ]
        },
        {
            "group": "Exception propagation from overloaded validate",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock(ExecutionContext.class)"
                        },
                        {
                            "parameter": "node",
                            "value": "ObjectNodeFactory.createObjectNode()"
                        }
                    ],
                    "expected": "RuntimeException with message \"forced error\" is thrown",
                    "description": "A concrete subclass of BaseJsonValidator overrides the four‑argument validate method to throw a RuntimeException. The test asserts that the single‑argument validate method does not catch the exception and propagates it unchanged."
                }
            ]
        },
        {
            "group": "Empty result handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock(ExecutionContext.class)"
                        },
                        {
                            "parameter": "node",
                            "value": "ObjectNodeFactory.createObjectNode()"
                        }
                    ],
                    "expected": "An empty Set<ValidationMessage> (size == 0)",
                    "description": "The overridden four‑argument validate returns Collections.emptySet(). The test confirms that BaseJsonValidator.validate returns an empty set rather than null or a non‑empty collection."
                }
            ]
        },
        {
            "group": "Handling of non‑object JsonNode (TextNode)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock(ExecutionContext.class)"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNodeFactory.instance.textNode(\"sample\")"
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing exactly one message whose text includes \"TEXT\"",
                    "description": "A subclass implementation records the runtime class name of the node argument and returns a ValidationMessage containing that name. Supplying a TextNode ensures the validator works for primitive nodes and that the correct node instance is passed through."
                }
            ]
        },
        {
            "group": "Idempotent delegation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock(ExecutionContext.class)"
                        },
                        {
                            "parameter": "node",
                            "value": "ObjectNodeFactory.createObjectNode().put(\"id\", 1)"
                        }
                    ],
                    "expected": "Two consecutive calls to validate return equal Set<ValidationMessage> instances (equals() true) and the overloaded validate is invoked exactly twice.",
                    "description": "Ensures that repeated invocations with the same arguments delegate each time and produce identical results, confirming no hidden state is cached inside BaseJsonValidator.validate."
                }
            ]
        }
    ]
}