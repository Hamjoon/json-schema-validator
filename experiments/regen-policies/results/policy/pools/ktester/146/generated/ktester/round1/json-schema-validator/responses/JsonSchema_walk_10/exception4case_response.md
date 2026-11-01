{
    "test_cases": [
        {
            "group": "Null validationContext causes NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The JsonSchema instance is constructed with a null ValidationContext. When walk() accesses validationContext.getConfig(), a NullPointerException is thrown."
                }
            ]
        },
        {
            "group": "Null Config in ValidationContext causes NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": false
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The ValidationContext inside JsonSchema returns null for getConfig(). The subsequent call to getKeywordWalkListenerRunner() triggers a NullPointerException."
                }
            ]
        },
        {
            "group": "Null KeywordWalkListenerRunner causes NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The Config object returns null for getKeywordWalkListenerRunner(). When walk() attempts to invoke runPreWalkListeners, a NullPointerException is thrown."
                }
            ]
        },
        {
            "group": "Pre‑walk listener throws RuntimeException – post‑walk still invoked",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "The pre‑walk listener (runPreWalkListeners) throws a RuntimeException. The finally block must still invoke the post‑walk listener exactly once before the exception propagates."
                }
            ]
        },
        {
            "group": "Post‑walk listener throws RuntimeException – exception propagates",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": false
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "The post‑walk listener (runPostWalkListeners) throws a RuntimeException. The exception should bubble out of JsonSchema.walk() (the finally block does not swallow it)."
                }
            ]
        },
        {
            "group": "Validator returns null evaluationPath – NullPointerException before pre‑walk",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "A registered JsonValidator returns null from getEvaluationPath(). The subsequent call to evaluationPathWithKeyword.getName(-1) triggers a NullPointerException."
                }
            ]
        },
        {
            "group": "Empty evaluationPath causes getName(-1) IndexOutOfBoundsException – post‑walk still invoked",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": false
                        }
                    ],
                    "expected": "IndexOutOfBoundsException",
                    "description": "The validator provides a JsonNodePath with no elements. Calling getName(-1) throws an IndexOutOfBoundsException. The finally block must still execute the post‑walk listener before the exception propagates."
                }
            ]
        }
    ]
}