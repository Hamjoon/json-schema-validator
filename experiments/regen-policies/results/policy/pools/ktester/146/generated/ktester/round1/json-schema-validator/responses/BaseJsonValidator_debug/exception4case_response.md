{
    "testCases": [
        {
            "group": "debug method - null logger",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "logger",
                            "value": "null"
                        },
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext where getExecutionConfig().isDebugEnabled() returns true"
                        },
                        {
                            "parameter": "node",
                            "value": "mock JsonNode where toString() returns \"{\\\"field\\\":1}\""
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mock JsonNode where toString() returns \"{\\\"root\\\":true}\""
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mock JsonNodePath where toString() returns \"/data/item\""
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When logger is null the method should throw NullPointerException before any logger method is invoked."
                }
            ]
        },
        {
            "group": "debug method - null executionContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "logger",
                            "value": "mock Logger where isDebugEnabled() returns true"
                        },
                        {
                            "parameter": "executionContext",
                            "value": "null"
                        },
                        {
                            "parameter": "node",
                            "value": "mock JsonNode where toString() returns \"{\\\"field\\\":1}\""
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mock JsonNode where toString() returns \"{\\\"root\\\":true}\""
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mock JsonNodePath where toString() returns \"/data/item\""
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When executionContext is null the call to executionContext.getExecutionConfig() should raise NullPointerException."
                }
            ]
        },
        {
            "group": "debug method - null executionConfig",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "logger",
                            "value": "mock Logger where isDebugEnabled() returns true"
                        },
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext where getExecutionConfig() returns null"
                        },
                        {
                            "parameter": "node",
                            "value": "mock JsonNode where toString() returns \"{\\\"field\\\":1}\""
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mock JsonNode where toString() returns \"{\\\"root\\\":true}\""
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mock JsonNodePath where toString() returns \"/data/item\""
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If ExecutionContext.getExecutionConfig() returns null, invoking isDebugEnabled() on it should throw NullPointerException."
                }
            ]
        },
        {
            "group": "debug method - null node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "logger",
                            "value": "mock Logger where isDebugEnabled() returns true"
                        },
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext where getExecutionConfig().isDebugEnabled() returns true"
                        },
                        {
                            "parameter": "node",
                            "value": "null"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mock JsonNode where toString() returns \"{\\\"root\\\":true}\""
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mock JsonNodePath where toString() returns \"/data/item\""
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When node is null, the call to node.toString() inside the debug block should raise NullPointerException."
                }
            ]
        },
        {
            "group": "debug method - null rootNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "logger",
                            "value": "mock Logger where isDebugEnabled() returns true"
                        },
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext where getExecutionConfig().isDebugEnabled() returns true"
                        },
                        {
                            "parameter": "node",
                            "value": "mock JsonNode where toString() returns \"{\\\"field\\\":1}\""
                        },
                        {
                            "parameter": "rootNode",
                            "value": "null"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mock JsonNodePath where toString() returns \"/data/item\""
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When rootNode is null, the call to rootNode.toString() should raise NullPointerException."
                }
            ]
        },
        {
            "group": "debug method - null instanceLocation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "logger",
                            "value": "mock Logger where isDebugEnabled() returns true"
                        },
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext where getExecutionConfig().isDebugEnabled() returns true"
                        },
                        {
                            "parameter": "node",
                            "value": "mock JsonNode where toString() returns \"{\\\"field\\\":1}\""
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mock JsonNode where toString() returns \"{\\\"root\\\":true}\""
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When instanceLocation is null, the call to instanceLocation.toString() should raise NullPointerException."
                }
            ]
        },
        {
            "group": "debug method - logger debug disabled but other nulls present",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "logger",
                            "value": "mock Logger where isDebugEnabled() returns false"
                        },
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext where getExecutionConfig().isDebugEnabled() returns true"
                        },
                        {
                            "parameter": "node",
                            "value": "null"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "null"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "null"
                        }
                    ],
                    "expected": "No exception (method returns silently)",
                    "description": "Even if node, rootNode and instanceLocation are null, when logger.isDebugEnabled() is false the method should short‑circuit before accessing them, thus no exception is thrown."
                }
            ]
        }
    ]
}