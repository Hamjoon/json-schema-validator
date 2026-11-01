{
    "test_cases": [
        {
            "group": "NullExecutionContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "null"
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
                    "expected": "NullPointerException is thrown when the method attempts to call executionContext.getCollectorContext()",
                    "description": "The supplied ExecutionContext is null. The walk method should immediately throw a NullPointerException before any listener or validator interaction."
                }
            ]
        },
        {
            "group": "NullCollectorContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext_withNullCollector"
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
                    "expected": "NullPointerException is thrown when collectorContext.enterDynamicScope(this) is invoked",
                    "description": "The ExecutionContext returns a null CollectorContext. The walk method should raise a NullPointerException at the point where it tries to enter a dynamic scope."
                }
            ]
        },
        {
            "group": "NullValidatorsList",
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
                    "expected": "NullPointerException is thrown because getValidators() returns null and the for‑each loop attempts to iterate over it",
                    "description": "The JsonSchema instance is configured so that getValidators() returns null. The walk method should not silently treat it as empty; it will throw a NullPointerException when the enhanced for‑loop starts."
                }
            ]
        },
        {
            "group": "NullValidatorInList",
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
                    "expected": "NullPointerException is thrown when the loop attempts to call v.getEvaluationPath() on a null validator",
                    "description": "The validators list contains a null element. The walk method should raise a NullPointerException at the first dereference of the validator."
                }
            ]
        },
        {
            "group": "NullEvaluationPath",
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
                    "expected": "NullPointerException is thrown when evaluationPathWithKeyword.getName(-1) is called on a null evaluation path",
                    "description": "A validator returns null from getEvaluationPath(). The walk method should throw a NullPointerException while trying to retrieve the keyword name."
                }
            ]
        },
        {
            "group": "EmptyEvaluationPathIndexOutOfBounds",
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
                    "expected": "IndexOutOfBoundsException is thrown when getName(-1) is invoked on an empty JsonNodePath",
                    "description": "The validator's getEvaluationPath() returns an empty JsonNodePath. Calling getName(-1) should raise an IndexOutOfBoundsException, which must propagate out of walk."
                }
            ]
        },
        {
            "group": "NullSchemaNodeDuringPostWalk",
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
                    "expected": "NullPointerException is thrown inside runPostWalkListeners because this.schemaNode is null",
                    "description": "The JsonSchema instance has its schemaNode field set to null. When the finally block invokes runPostWalkListeners, the null schemaNode causes a NullPointerException."
                }
            ]
        },
        {
            "group": "PostWalkListenerThrowsException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext_postWalkException"
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
                    "expected": "RuntimeException propagates after the post‑walk listeners are invoked; the method does not return an error set",
                    "description": "runPostWalkListeners throws a RuntimeException. The walk method must still have entered the finally block, invoke the listener (which throws), and let the exception bubble up."
                }
            ]
        }
    ]
}