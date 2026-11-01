{
    "testCases": [
        {
            "group": "$ref handling - UnsupportedEncodingException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "objectNode({\"ref\":\"value\"})"
                        },
                        {
                            "parameter": "format",
                            "value": "new SimpleStringFormat()"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "UnsupportedEncodingException",
                    "description": "The schema contains a $ref with an illegal URL encoding (e.g., \"#/components/schemas/Invalid%ZZ\"). During validation the resolver calls URLDecoder.decode which throws UnsupportedEncodingException. The validate method must propagate this exception."
                }
            ]
        },
        {
            "group": "$ref handling - IOException on external schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "objectNode({\"ref\":\"value\"})"
                        },
                        {
                            "parameter": "format",
                            "value": "new SimpleStringFormat()"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "IOException",
                    "description": "The schema contains an external $ref that points to a non‑existent file or an unreachable URL (e.g., \"file:/nonexistent/schema.json\"). When the validator tries to read the referenced schema it throws an IOException, which should be propagated by the validate method."
                }
            ]
        },
        {
            "group": "ExecutionContextCustomizer exception propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "objectNode({\"age\":30})"
                        },
                        {
                            "parameter": "format",
                            "value": "new SimpleStringFormat()"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> { throw new IllegalStateException(\"customizer failure\"); }"
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "A custom ExecutionContextCustomizer throws an unchecked exception while configuring the ExecutionContext. The validate method must not swallow the exception; it should propagate it to the caller."
                }
            ]
        },
        {
            "group": "OutputFormat exception propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "objectNode({\"age\":30})"
                        },
                        {
                            "parameter": "format",
                            "value": "new FaultyFormat()" 
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "The supplied OutputFormat implementation throws a RuntimeException (e.g., in its `apply` method). The validate method should let this exception bubble up rather than converting it to a validation message."
                }
            ]
        },
        {
            "group": "Invalid schema definition - IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "objectNode({\"age\":30})"
                        },
                        {
                            "parameter": "format",
                            "value": "new SimpleStringFormat()"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The JsonSchema instance is created from a malformed schema JSON (e.g., a property \"type\" with an unsupported value \"unknown\"). During validator initialization an IllegalArgumentException is thrown, which must be propagated by the validate method."
                }
            ]
        }
    ]
}