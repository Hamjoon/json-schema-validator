{
    "testCases": [
        {
            "group": "OneOfValidator.walk - shouldValidateSchema = false, no schemas configured",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/anyPath\""
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "false"
                        }
                    ],
                    "expected": "Empty Set. Since the validator has no child schemas, the method must not add any ValidationMessage and simply return an empty set.",
                    "description": "When shouldValidateSchema is false and the internal `schemas` list is empty, walk() should skip validation and return an empty set without throwing or delegating to any schema."
                }
            ]
        },
        {
            "group": "OneOfValidator.walk - shouldValidateSchema = false, multiple child schemas",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/path/to/element\""
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "false"
                        }
                    ],
                    "expected": "Empty Set; each child schema's walk(executionContext, node, rootNode, at, false) is invoked exactly once.",
                    "description": "When shouldValidateSchema is false and the validator holds several JsonSchema instances, walk() must delegate the call to every child schema with the same parameters. The method itself still returns an empty set because it never aggregates messages from the delegated walks."
                }
            ]
        },
        {
            "group": "OneOfValidator.walk - shouldValidateSchema = false, child schema throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/errorPath\""
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "false"
                        }
                    ],
                    "expected": "RuntimeException propagated from the child schema's walk method.",
                    "description": "If a delegated schema's walk() throws an unchecked exception, OneOfValidator.walk() must not catch it; the exception should propagate to the caller. This verifies that the method does not swallow runtime errors when delegating."
                }
            ]
        },
        {
            "group": "OneOfValidator.walk - shouldValidateSchema = true, null node argument",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "null"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/nullNodePath\""
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "NullPointerException (or any exception thrown by validate when node is null).",
                    "description": "When shouldValidateSchema is true and the `node` argument is null, the method forwards the call to validate(). The underlying validate implementation is expected to reject a null node, resulting in a NullPointerException. The test confirms that walk() does not mask the exception."
                }
            ]
        }
    ]
}